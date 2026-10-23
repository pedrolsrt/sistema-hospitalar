package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.ConsultaEspecializadaRequest;
import br.pucminas.hospital.dto.ConsultaResponse;
import br.pucminas.hospital.dto.ConsultaRotinaRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.mapper.ConsultaMapper;
import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.ConsultaEspecializada;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.SituacaoConsulta;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agendamento e acompanhamento de consultas. As mudanças de situação são feitas pela própria consulta,
 * que segue a máquina de estados de {@link SituacaoConsulta}.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaService {

	private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

	private final ConsultaRepository repository;
	private final PacienteRepository pacienteRepository;
	private final ProfissionalSaudeRepository profissionalRepository;
	private final ConsultaMapper mapper;
	private final Clock relogio;
	private final Duration duracao;

	public ConsultaService(ConsultaRepository repository, PacienteRepository pacienteRepository,
			ProfissionalSaudeRepository profissionalRepository, ConsultaMapper mapper, Clock relogio,
			@Value("${hospital.consulta.duracao-minutos}") long duracaoMinutos) {
		this.repository = repository;
		this.pacienteRepository = pacienteRepository;
		this.profissionalRepository = profissionalRepository;
		this.mapper = mapper;
		this.relogio = relogio;
		this.duracao = Duration.ofMinutes(duracaoMinutos);
	}

	/**
	 * Agenda uma consulta de rotina. Ver {@link #agendar(Consulta)} para as regras.
	 */
	@Transactional
	public ConsultaResponse agendarRotina(ConsultaRotinaRequest request) {
		Paciente paciente = Busca.paciente(pacienteRepository, request.pacienteId());
		ProfissionalSaude profissional = Busca.profissional(profissionalRepository, request.profissionalId());
		return agendar(new ConsultaRotina(paciente, profissional, request.data(), request.horario(),
				request.motivo().strip()));
	}

	/**
	 * Agenda uma consulta especializada, que exige um médico especialista da área. Ver
	 * {@link #agendar(Consulta)} para as demais regras.
	 */
	@Transactional
	public ConsultaResponse agendarEspecializada(ConsultaEspecializadaRequest request) {
		Paciente paciente = Busca.paciente(pacienteRepository, request.pacienteId());
		ProfissionalSaude profissional = Busca.profissional(profissionalRepository, request.profissionalId());
		return agendar(new ConsultaEspecializada(paciente, profissional, request.data(), request.horario(),
				request.motivo().strip(), request.areaAtuacao()));
	}

	/**
	 * Regras do agendamento: data e horário no futuro; as condições de realização da modalidade (paciente
	 * e profissional ativos e, na especializada, especialista da área); e nenhuma sobreposição com
	 * consultas ativas do profissional ou do paciente, considerando a duração configurada.
	 *
	 * @throws DataInvalidaException se a data e o horário não estiverem no futuro
	 * @throws RegraNegocioException se a consulta não puder ser realizada ou o horário estiver ocupado
	 */
	private ConsultaResponse agendar(Consulta consulta) {
		LocalDateTime inicio = consulta.getDataHoraReferencia();
		if (!inicio.isAfter(LocalDateTime.now(relogio))) {
			throw new DataInvalidaException("A consulta deve ser agendada para uma data e horário futuros; foi informado "
					+ FORMATO_DATA_HORA.format(inicio) + ".");
		}
		consulta.validarRealizacao();
		exigirHorarioLivre(consulta);
		return mapper.paraResponse(repository.save(consulta));
	}

	// As consultas que podem se sobrepor começam a menos de uma duração antes ou depois do início pedido;
	// por isso a busca cobre também o dia anterior ou o seguinte quando esse intervalo cruza a meia-noite.
	private void exigirHorarioLivre(Consulta consulta) {
		LocalDateTime inicio = consulta.getDataHoraReferencia();
		LocalDate ultimoDia = inicio.plus(duracao).toLocalDate();
		for (LocalDate dia = inicio.minus(duracao).toLocalDate(); !dia.isAfter(ultimoDia); dia = dia.plusDays(1)) {
			ProfissionalSaude profissional = consulta.getProfissionalResponsavel();
			Optional<Consulta> doProfissional = sobreposta(
					repository.findAtivasDoProfissionalNaData(profissional.getId(), dia), inicio);
			if (doProfissional.isPresent()) {
				throw horarioOcupado("O profissional " + profissional.getNome(), doProfissional.get());
			}
			Paciente paciente = consulta.getPaciente();
			Optional<Consulta> doPaciente = sobreposta(repository.findAtivasDoPacienteNaData(paciente.getId(), dia),
					inicio);
			if (doPaciente.isPresent()) {
				throw horarioOcupado("O paciente " + paciente.getNome(), doPaciente.get());
			}
		}
	}

	// Duas consultas de mesma duração se sobrepõem se cada uma começa antes do fim da outra.
	private Optional<Consulta> sobreposta(List<Consulta> consultas, LocalDateTime inicio) {
		LocalDateTime fim = inicio.plus(duracao);
		return consultas.stream().filter(outra -> {
			LocalDateTime inicioOutra = outra.getDataHoraReferencia();
			return inicio.isBefore(inicioOutra.plus(duracao)) && inicioOutra.isBefore(fim);
		}).findFirst();
	}

	private RegraNegocioException horarioOcupado(String quem, Consulta existente) {
		return new RegraNegocioException(quem + " já tem consulta em "
				+ FORMATO_DATA_HORA.format(existente.getDataHoraReferencia()) + ", que se sobrepõe ao horário pedido "
				+ "(cada consulta dura " + duracao.toMinutes() + " minutos).");
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver agendada
	 */
	@Transactional
	public ConsultaResponse confirmar(Long id) {
		return alterar(id, Consulta::confirmar);
	}

	/**
	 * Inicia o atendimento, verificando de novo se a consulta pode ser realizada.
	 *
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver confirmada
	 * @throws RegraNegocioException se a consulta não puder mais ser realizada
	 */
	@Transactional
	public ConsultaResponse iniciarAtendimento(Long id) {
		return alterar(id, Consulta::iniciarAtendimento);
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver em atendimento
	 */
	@Transactional
	public ConsultaResponse concluir(Long id) {
		return alterar(id, Consulta::concluir);
	}

	/**
	 * Cancela a consulta. Remarcar significa agendar uma nova.
	 *
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver agendada ou confirmada
	 */
	@Transactional
	public ConsultaResponse cancelar(Long id) {
		return alterar(id, Consulta::cancelar);
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver agendada ou confirmada
	 */
	@Transactional
	public ConsultaResponse registrarNaoComparecimento(Long id) {
		return alterar(id, Consulta::registrarNaoComparecimento);
	}

	/**
	 * @throws RegraNegocioException se a consulta não estiver em atendimento nem concluída
	 */
	@Transactional
	public ConsultaResponse registrarObservacoes(Long id, ObservacoesRequest request) {
		return alterar(id, consulta -> consulta.registrarObservacoes(request.texto().strip()));
	}

	/**
	 * @throws RecursoNaoEncontradoException se a consulta não existir
	 */
	public ConsultaResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	/**
	 * Lista as consultas em ordem de data e horário. Cada filtro é opcional (nulo = sem filtro).
	 */
	public List<ConsultaResponse> listar(SituacaoConsulta situacao, Long profissionalId, LocalDate data) {
		return repository.findComFiltros(situacao, profissionalId, data).stream().map(mapper::paraResponse).toList();
	}

	/**
	 * Agenda do profissional na data, com consultas em qualquer situação, em ordem de horário.
	 *
	 * @throws RecursoNaoEncontradoException se o profissional não existir
	 */
	public List<ConsultaResponse> agendaDoProfissional(Long profissionalId, LocalDate data) {
		Busca.profissional(profissionalRepository, profissionalId);
		return repository.findAgendaDoProfissional(profissionalId, data).stream().map(mapper::paraResponse).toList();
	}

	private ConsultaResponse alterar(Long id, Consumer<Consulta> alteracao) {
		Consulta consulta = buscar(id);
		alteracao.accept(consulta);
		return mapper.paraResponse(consulta);
	}

	private Consulta buscar(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Consulta não encontrada (id " + id + ")."));
	}

}
