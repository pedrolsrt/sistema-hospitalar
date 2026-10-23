package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.ProfissionalSaudeAtualizacaoRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeResponse;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.ProfissionalSaudeMapper;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.repository.AtendimentoEmergenciaRepository;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.InternacaoRepository;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de profissionais de saúde. Profissionais não são excluídos: são inativados, para preservar
 * o histórico dos atendimentos.
 */
@Service
@Transactional(readOnly = true)
public class ProfissionalSaudeService {

	private final ProfissionalSaudeRepository repository;
	private final ConsultaRepository consultaRepository;
	private final AtendimentoEmergenciaRepository emergenciaRepository;
	private final ProcedimentoCirurgicoRepository procedimentoRepository;
	private final InternacaoRepository internacaoRepository;
	private final ProfissionalSaudeMapper mapper;

	public ProfissionalSaudeService(ProfissionalSaudeRepository repository, ConsultaRepository consultaRepository,
			AtendimentoEmergenciaRepository emergenciaRepository, ProcedimentoCirurgicoRepository procedimentoRepository,
			InternacaoRepository internacaoRepository, ProfissionalSaudeMapper mapper) {
		this.repository = repository;
		this.consultaRepository = consultaRepository;
		this.emergenciaRepository = emergenciaRepository;
		this.procedimentoRepository = procedimentoRepository;
		this.internacaoRepository = internacaoRepository;
		this.mapper = mapper;
	}

	/**
	 * Cadastra um profissional ativo da categoria informada.
	 *
	 * @throws DadoJaCadastradoException se já houver profissional com o mesmo registro profissional
	 */
	@Transactional
	public ProfissionalSaudeResponse cadastrar(ProfissionalSaudeRequest request) {
		ProfissionalSaude profissional = mapper.paraEntidade(request);
		if (repository.existsByRegistroProfissional(profissional.getRegistroProfissional())) {
			throw new DadoJaCadastradoException("Já existe um profissional cadastrado com o registro "
					+ profissional.getRegistroProfissional() + ".");
		}
		return mapper.paraResponse(repository.save(profissional));
	}

	/**
	 * Atualiza nome, telefone e e-mail. Registro, categoria e área não mudam.
	 *
	 * @throws RecursoNaoEncontradoException se o profissional não existir
	 */
	@Transactional
	public ProfissionalSaudeResponse atualizar(Long id, ProfissionalSaudeAtualizacaoRequest request) {
		ProfissionalSaude profissional = buscar(id);
		mapper.atualizar(profissional, request);
		return mapper.paraResponse(profissional);
	}

	/**
	 * @throws RecursoNaoEncontradoException se o profissional não existir
	 */
	public ProfissionalSaudeResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	/**
	 * Lista os profissionais em ordem de nome, todos ou só os ativos.
	 */
	public List<ProfissionalSaudeResponse> listar(boolean somenteAtivos) {
		List<ProfissionalSaude> profissionais = somenteAtivos
				? repository.findByAtivoTrueOrderByNomeAsc()
				: repository.findAll(Sort.by("nome"));
		return profissionais.stream().map(mapper::paraResponse).toList();
	}

	/**
	 * Lista os profissionais ativos que atuam na área: especialistas pela área de atuação e cirurgiões
	 * pela área cirúrgica.
	 */
	public List<ProfissionalSaudeResponse> listarPorArea(AreaAtuacao area) {
		return repository.findAtivosPorArea(area).stream().map(mapper::paraResponse).toList();
	}

	/**
	 * Inativa o profissional. Não é permitido enquanto houver consulta, emergência, procedimento ou
	 * internação ativos sob a sua responsabilidade. Não tem efeito se ele já estiver inativo.
	 *
	 * @throws RecursoNaoEncontradoException se o profissional não existir
	 * @throws RegraNegocioException se houver atendimento ativo sob sua responsabilidade, informando quais
	 */
	@Transactional
	public ProfissionalSaudeResponse inativar(Long id) {
		ProfissionalSaude profissional = buscar(id);
		if (profissional.isAtivo()) {
			new Impedimentos()
					.se(consultaRepository.existeAtivaDoProfissional(id), "consulta agendada, confirmada ou em atendimento")
					.se(emergenciaRepository.existeAtivoDoProfissional(id),
							"atendimento de emergência aguardando avaliação ou em atendimento")
					.se(procedimentoRepository.existeAtivoDoProfissional(id),
							"procedimento cirúrgico agendado ou em andamento")
					.se(internacaoRepository.existsByProfissionalResponsavelIdAndDataEfetivaAltaIsNull(id),
							"internação ativa")
					.exigirNenhum("O profissional " + profissional.getNome()
							+ " não pode ser inativado porque tem sob sua responsabilidade");
			profissional.inativar();
		}
		return mapper.paraResponse(profissional);
	}

	/**
	 * Reativa o profissional. Não tem efeito se ele já estiver ativo.
	 *
	 * @throws RecursoNaoEncontradoException se o profissional não existir
	 */
	@Transactional
	public ProfissionalSaudeResponse reativar(Long id) {
		ProfissionalSaude profissional = buscar(id);
		profissional.reativar();
		return mapper.paraResponse(profissional);
	}

	private ProfissionalSaude buscar(Long id) {
		return Busca.profissional(repository, id);
	}

}
