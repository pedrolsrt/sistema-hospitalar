package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.PacienteAtualizacaoRequest;
import br.pucminas.hospital.dto.PacienteRequest;
import br.pucminas.hospital.dto.PacienteResponse;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.PacienteMapper;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.repository.AtendimentoEmergenciaRepository;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.InternacaoRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de pacientes. Pacientes não são excluídos: são inativados, para preservar o histórico.
 */
@Service
@Transactional(readOnly = true)
public class PacienteService {

	private final PacienteRepository repository;
	private final InternacaoRepository internacaoRepository;
	private final ConsultaRepository consultaRepository;
	private final AtendimentoEmergenciaRepository emergenciaRepository;
	private final ProcedimentoCirurgicoRepository procedimentoRepository;
	private final PacienteMapper mapper;

	public PacienteService(PacienteRepository repository, InternacaoRepository internacaoRepository,
			ConsultaRepository consultaRepository, AtendimentoEmergenciaRepository emergenciaRepository,
			ProcedimentoCirurgicoRepository procedimentoRepository, PacienteMapper mapper) {
		this.repository = repository;
		this.internacaoRepository = internacaoRepository;
		this.consultaRepository = consultaRepository;
		this.emergenciaRepository = emergenciaRepository;
		this.procedimentoRepository = procedimentoRepository;
		this.mapper = mapper;
	}

	/**
	 * Cadastra um paciente ativo.
	 *
	 * @throws DadoJaCadastradoException se já houver paciente com o mesmo CPF
	 */
	@Transactional
	public PacienteResponse cadastrar(PacienteRequest request) {
		Paciente paciente = mapper.paraEntidade(request);
		if (repository.existsByCpf(paciente.getCpf())) {
			throw new DadoJaCadastradoException(
					"Já existe um paciente cadastrado com o CPF " + formatarCpf(paciente.getCpf()) + ".");
		}
		return mapper.paraResponse(repository.save(paciente));
	}

	/**
	 * Atualiza os dados do paciente. O CPF não é alterado.
	 *
	 * @throws RecursoNaoEncontradoException se o paciente não existir
	 */
	@Transactional
	public PacienteResponse atualizar(Long id, PacienteAtualizacaoRequest request) {
		Paciente paciente = buscar(id);
		mapper.atualizar(paciente, request);
		return mapper.paraResponse(paciente);
	}

	/**
	 * @throws RecursoNaoEncontradoException se o paciente não existir
	 */
	public PacienteResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	/**
	 * Lista os pacientes em ordem de nome, todos ou só os ativos.
	 */
	public List<PacienteResponse> listar(boolean somenteAtivos) {
		List<Paciente> pacientes = somenteAtivos
				? repository.findByAtivoTrueOrderByNomeAsc()
				: repository.findAll(Sort.by("nome"));
		return pacientes.stream().map(mapper::paraResponse).toList();
	}

	/**
	 * Inativa o paciente. Não é permitido enquanto ele tiver internação ativa ou consulta, emergência ou
	 * procedimento em situação ativa. Não tem efeito se ele já estiver inativo.
	 *
	 * @throws RecursoNaoEncontradoException se o paciente não existir
	 * @throws RegraNegocioException se houver atendimento ativo, informando quais
	 */
	@Transactional
	public PacienteResponse inativar(Long id) {
		Paciente paciente = buscar(id);
		if (paciente.isAtivo()) {
			new Impedimentos()
					.se(internacaoRepository.existsByPacienteIdAndDataEfetivaAltaIsNull(id), "internação ativa")
					.se(consultaRepository.existeAtivaDoPaciente(id), "consulta agendada, confirmada ou em atendimento")
					.se(emergenciaRepository.existeAtivoDoPaciente(id),
							"atendimento de emergência aguardando avaliação ou em atendimento")
					.se(procedimentoRepository.existeAtivoDoPaciente(id), "procedimento cirúrgico agendado ou em andamento")
					.exigirNenhum("O paciente " + paciente.getNome() + " não pode ser inativado porque tem");
			paciente.inativar();
		}
		return mapper.paraResponse(paciente);
	}

	/**
	 * Reativa o paciente. Não tem efeito se ele já estiver ativo.
	 *
	 * @throws RecursoNaoEncontradoException se o paciente não existir
	 */
	@Transactional
	public PacienteResponse reativar(Long id) {
		Paciente paciente = buscar(id);
		paciente.reativar();
		return mapper.paraResponse(paciente);
	}

	private Paciente buscar(Long id) {
		return Busca.paciente(repository, id);
	}

	private static String formatarCpf(String cpf) {
		return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
	}

}
