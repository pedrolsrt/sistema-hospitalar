package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.ConsultaEspecializadaRequest;
import br.pucminas.hospital.dto.ConsultaResponse;
import br.pucminas.hospital.dto.ConsultaRotinaRequest;
import br.pucminas.hospital.dto.EncaminhamentoRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.exception.AtendimentoIndisponivelException;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.ProfissionalIncompativelException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.mapper.ConsultaMapper;
import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.ConsultaEspecializada;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.FiltroAtendimentos;
import br.pucminas.hospital.model.OrdenacaoAtendimentos;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.SituacaoConsulta;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
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
	private final VerificadorDeDisponibilidade disponibilidade;

	public ConsultaService(ConsultaRepository repository, PacienteRepository pacienteRepository,
			ProfissionalSaudeRepository profissionalRepository, ConsultaMapper mapper, Clock relogio,
			VerificadorDeDisponibilidade disponibilidade) {
		this.repository = repository;
		this.pacienteRepository = pacienteRepository;
		this.profissionalRepository = profissionalRepository;
		this.mapper = mapper;
		this.relogio = relogio;
		this.disponibilidade = disponibilidade;
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
	 * e profissional ativos e, na especializada, especialista da área); e nenhuma sobreposição com consultas
	 * ou procedimentos ativos do profissional ou do paciente.
	 *
	 * @throws DataInvalidaException se a data e o horário não estiverem no futuro
	 * @throws ProfissionalIncompativelException se, na consulta especializada, o profissional não for especialista
	 *         da área
	 * @throws AtendimentoIndisponivelException se o horário estiver ocupado pelo profissional ou pelo paciente
	 * @throws RegraNegocioException se o paciente ou o profissional estiver inativo
	 */
	private ConsultaResponse agendar(Consulta consulta) {
		LocalDateTime inicio = consulta.getDataHoraReferencia();
		if (!inicio.isAfter(LocalDateTime.now(relogio))) {
			throw new DataInvalidaException("A consulta deve ser agendada para uma data e horário futuros; foi informado "
					+ FORMATO_DATA_HORA.format(inicio) + ".");
		}
		consulta.validarRealizacao();
		disponibilidade.exigirHorarioLivre(consulta);
		return mapper.paraResponse(repository.save(consulta));
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
	 * Encaminha o paciente para atendimento especializado. Para escolher o profissional, consulte os
	 * especialistas da área em {@code GET /profissionais?area=}.
	 *
	 * @throws ProfissionalIncompativelException se o profissional da consulta não for clínico geral
	 * @throws RegraNegocioException se a consulta não for de rotina ou não estiver em atendimento nem
	 *         concluída
	 */
	@Transactional
	public ConsultaResponse encaminhar(Long id, EncaminhamentoRequest request) {
		return alterar(id, consulta -> consulta.encaminhar(request.areaAtuacao()));
	}

	/**
	 * @throws RecursoNaoEncontradoException se a consulta não existir
	 */
	public ConsultaResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	/**
	 * Lista as consultas em ordem de data e horário. Cada filtro é opcional (nulo = sem filtro) e a filtragem
	 * é a mesma da busca de atendimentos, {@link FiltroAtendimentos}.
	 */
	public List<ConsultaResponse> listar(SituacaoConsulta situacao, Long profissionalId, LocalDate data) {
		return listar(new FiltroAtendimentos(null, profissionalId, situacao == null ? null : situacao.name(), null,
				data, data));
	}

	/**
	 * Agenda do profissional na data, com consultas em qualquer situação, em ordem de horário.
	 *
	 * @throws RecursoNaoEncontradoException se o profissional não existir
	 */
	public List<ConsultaResponse> agendaDoProfissional(Long profissionalId, LocalDate data) {
		Busca.profissional(profissionalRepository, profissionalId);
		return listar(new FiltroAtendimentos(null, profissionalId, null, null, data, data));
	}

	private List<ConsultaResponse> listar(FiltroAtendimentos filtro) {
		return repository.findAll().stream()
				.filter(filtro.comoPredicado())
				.sorted(OrdenacaoAtendimentos.DATA.comparador())
				.map(mapper::paraResponse)
				.toList();
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
