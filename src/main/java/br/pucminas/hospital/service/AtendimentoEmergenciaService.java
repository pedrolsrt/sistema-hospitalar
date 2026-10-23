package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.AtendimentoEmergenciaRequest;
import br.pucminas.hospital.dto.AtendimentoEmergenciaResponse;
import br.pucminas.hospital.dto.PrioridadeRequest;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.mapper.AtendimentoEmergenciaMapper;
import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.SituacaoEmergencia;
import br.pucminas.hospital.repository.AtendimentoEmergenciaRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atendimentos de emergência: registro da chegada, triagem (prioridade) e atendimento.
 */
@Service
@Transactional(readOnly = true)
public class AtendimentoEmergenciaService {

	private final AtendimentoEmergenciaRepository repository;
	private final PacienteRepository pacienteRepository;
	private final ProfissionalSaudeRepository profissionalRepository;
	private final AtendimentoEmergenciaMapper mapper;
	private final Clock relogio;

	public AtendimentoEmergenciaService(AtendimentoEmergenciaRepository repository,
			PacienteRepository pacienteRepository, ProfissionalSaudeRepository profissionalRepository,
			AtendimentoEmergenciaMapper mapper, Clock relogio) {
		this.repository = repository;
		this.pacienteRepository = pacienteRepository;
		this.profissionalRepository = profissionalRepository;
		this.mapper = mapper;
		this.relogio = relogio;
	}

	/**
	 * Registra a chegada do paciente com o profissional de plantão. A prioridade pode ficar para a
	 * avaliação; sem data e hora de chegada, vale o momento do registro.
	 *
	 * @throws RegraNegocioException se o paciente ou o profissional estiver inativo
	 */
	@Transactional
	public AtendimentoEmergenciaResponse registrarChegada(AtendimentoEmergenciaRequest request) {
		LocalDateTime chegada = request.dataHoraChegada() != null ? request.dataHoraChegada()
				: LocalDateTime.now(relogio);
		AtendimentoEmergencia emergencia = new AtendimentoEmergencia(
				Busca.paciente(pacienteRepository, request.pacienteId()),
				Busca.profissional(profissionalRepository, request.profissionalId()), request.prioridade(), chegada);
		emergencia.validarParticipantesAtivos();
		return mapper.paraResponse(repository.save(emergencia));
	}

	/**
	 * Define ou reclassifica a prioridade enquanto o atendimento não foi concluído.
	 *
	 * @throws RegraNegocioException se o atendimento já estiver concluído
	 */
	@Transactional
	public AtendimentoEmergenciaResponse definirPrioridade(Long id, PrioridadeRequest request) {
		return alterar(id, emergencia -> emergencia.definirPrioridade(request.prioridade()));
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se o atendimento não estiver aguardando avaliação
	 * @throws RegraNegocioException se a prioridade não estiver definida ou houver participante inativo
	 */
	@Transactional
	public AtendimentoEmergenciaResponse iniciar(Long id) {
		return alterar(id, AtendimentoEmergencia::iniciar);
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se o atendimento não estiver em andamento
	 */
	@Transactional
	public AtendimentoEmergenciaResponse concluir(Long id) {
		return alterar(id, AtendimentoEmergencia::concluir);
	}

	/**
	 * @throws RecursoNaoEncontradoException se o atendimento não existir
	 */
	public AtendimentoEmergenciaResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	/**
	 * Quem aguarda avaliação, na ordem de atendimento: prioridade mais grave primeiro, depois a chegada;
	 * sem prioridade definida, no fim.
	 */
	public List<AtendimentoEmergenciaResponse> filaDeAvaliacao() {
		return repository.findFilaDeAvaliacao().stream().map(mapper::paraResponse).toList();
	}

	public List<AtendimentoEmergenciaResponse> listarPorSituacao(SituacaoEmergencia situacao) {
		return repository.findBySituacaoOrderByDataHoraChegadaAsc(situacao).stream().map(mapper::paraResponse).toList();
	}

	private AtendimentoEmergenciaResponse alterar(Long id, Consumer<AtendimentoEmergencia> alteracao) {
		AtendimentoEmergencia emergencia = buscar(id);
		alteracao.accept(emergencia);
		return mapper.paraResponse(emergencia);
	}

	private AtendimentoEmergencia buscar(Long id) {
		return repository.findById(id).orElseThrow(
				() -> new RecursoNaoEncontradoException("Atendimento de emergência não encontrado (id " + id + ")."));
	}

}
