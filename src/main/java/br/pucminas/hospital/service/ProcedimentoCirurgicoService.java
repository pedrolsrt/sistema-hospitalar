package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.ConclusaoProcedimentoRequest;
import br.pucminas.hospital.dto.InicioProcedimentoRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoRequest;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoResponse;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.mapper.ProcedimentoCirurgicoMapper;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procedimentos cirúrgicos. As regras de quem pode operar e da sequência de situações ficam no próprio
 * procedimento.
 */
@Service
@Transactional(readOnly = true)
public class ProcedimentoCirurgicoService {

	private final ProcedimentoCirurgicoRepository repository;
	private final PacienteRepository pacienteRepository;
	private final ProfissionalSaudeRepository profissionalRepository;
	private final ProcedimentoCirurgicoMapper mapper;

	public ProcedimentoCirurgicoService(ProcedimentoCirurgicoRepository repository,
			PacienteRepository pacienteRepository, ProfissionalSaudeRepository profissionalRepository,
			ProcedimentoCirurgicoMapper mapper) {
		this.repository = repository;
		this.pacienteRepository = pacienteRepository;
		this.profissionalRepository = profissionalRepository;
		this.mapper = mapper;
	}

	/**
	 * Agenda o procedimento.
	 *
	 * @throws RegraNegocioException se o profissional não for um cirurgião ativo ou o paciente estiver inativo
	 */
	@Transactional
	public ProcedimentoCirurgicoResponse agendar(ProcedimentoCirurgicoRequest request) {
		ProcedimentoCirurgico procedimento = new ProcedimentoCirurgico(
				Busca.paciente(pacienteRepository, request.pacienteId()),
				Busca.profissional(profissionalRepository, request.cirurgiaoId()), request.descricao().strip(),
				request.tipoCirurgia(), request.dataHoraPrevista());
		if (request.observacoesPreOperatorias() != null && !request.observacoesPreOperatorias().isBlank()) {
			procedimento.registrarObservacoesPreOperatorias(request.observacoesPreOperatorias().strip());
		}
		return mapper.paraResponse(repository.save(procedimento));
	}

	/**
	 * Inicia o procedimento, verificando de novo se ele pode ser realizado.
	 *
	 * @throws TransicaoSituacaoInvalidaException se o procedimento não estiver agendado
	 * @throws RegraNegocioException se o procedimento não puder mais ser realizado
	 */
	@Transactional
	public ProcedimentoCirurgicoResponse iniciar(Long id, InicioProcedimentoRequest request) {
		return alterar(id, procedimento -> procedimento.iniciar(request.dataHoraInicio()));
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se o procedimento não estiver em andamento
	 * @throws DataInvalidaException se o fim for anterior ao início
	 */
	@Transactional
	public ProcedimentoCirurgicoResponse concluir(Long id, ConclusaoProcedimentoRequest request) {
		return alterar(id, procedimento -> procedimento.concluir(request.dataHoraFim()));
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se o procedimento não estiver agendado
	 */
	@Transactional
	public ProcedimentoCirurgicoResponse cancelar(Long id) {
		return alterar(id, ProcedimentoCirurgico::cancelar);
	}

	@Transactional
	public ProcedimentoCirurgicoResponse registrarObservacoesPreOperatorias(Long id, ObservacoesRequest request) {
		return alterar(id, procedimento -> procedimento.registrarObservacoesPreOperatorias(request.texto().strip()));
	}

	/**
	 * @throws RegraNegocioException se o procedimento ainda não tiver sido iniciado
	 */
	@Transactional
	public ProcedimentoCirurgicoResponse registrarObservacoesPosOperatorias(Long id, ObservacoesRequest request) {
		return alterar(id, procedimento -> procedimento.registrarObservacoesPosOperatorias(request.texto().strip()));
	}

	/**
	 * @throws RecursoNaoEncontradoException se o procedimento não existir
	 */
	public ProcedimentoCirurgicoResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	/**
	 * Procedimentos previstos para a data, em qualquer situação, em ordem de horário.
	 */
	public List<ProcedimentoCirurgicoResponse> listarPorData(LocalDate data) {
		return repository.findPrevistosNaData(data).stream().map(mapper::paraResponse).toList();
	}

	/**
	 * Procedimentos concluídos pelo cirurgião, do mais recente para o mais antigo.
	 *
	 * @throws RecursoNaoEncontradoException se o profissional não existir
	 */
	public List<ProcedimentoCirurgicoResponse> listarRealizadosPorCirurgiao(Long cirurgiaoId) {
		Busca.profissional(profissionalRepository, cirurgiaoId);
		return repository.findConcluidosDoCirurgiao(cirurgiaoId).stream().map(mapper::paraResponse).toList();
	}

	private ProcedimentoCirurgicoResponse alterar(Long id, Consumer<ProcedimentoCirurgico> alteracao) {
		ProcedimentoCirurgico procedimento = buscar(id);
		alteracao.accept(procedimento);
		return mapper.paraResponse(procedimento);
	}

	private ProcedimentoCirurgico buscar(Long id) {
		return repository.findById(id).orElseThrow(
				() -> new RecursoNaoEncontradoException("Procedimento cirúrgico não encontrado (id " + id + ")."));
	}

}
