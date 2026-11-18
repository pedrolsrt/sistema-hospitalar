package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.SalaCirurgicaRequest;
import br.pucminas.hospital.dto.SalaCirurgicaResponse;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.SalaCirurgicaMapper;
import br.pucminas.hospital.model.SalaCirurgica;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import br.pucminas.hospital.repository.SalaCirurgicaRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de salas cirúrgicas. Salas não são excluídas: são inativadas, para preservar o histórico dos
 * procedimentos.
 */
@Service
@Transactional(readOnly = true)
public class SalaCirurgicaService {

	private final SalaCirurgicaRepository repository;
	private final ProcedimentoCirurgicoRepository procedimentoRepository;
	private final SalaCirurgicaMapper mapper;

	public SalaCirurgicaService(SalaCirurgicaRepository repository,
			ProcedimentoCirurgicoRepository procedimentoRepository, SalaCirurgicaMapper mapper) {
		this.repository = repository;
		this.procedimentoRepository = procedimentoRepository;
		this.mapper = mapper;
	}

	/**
	 * Cadastra uma sala ativa.
	 *
	 * @throws DadoJaCadastradoException se já houver sala com a mesma identificação
	 */
	@Transactional
	public SalaCirurgicaResponse cadastrar(SalaCirurgicaRequest request) {
		SalaCirurgica sala = mapper.paraEntidade(request);
		exigirIdentificacaoLivre(sala.getIdentificacao(), null);
		return mapper.paraResponse(repository.save(sala));
	}

	/**
	 * Atualiza a identificação da sala.
	 *
	 * @throws RecursoNaoEncontradoException se a sala não existir
	 * @throws DadoJaCadastradoException se a nova identificação já pertencer a outra sala
	 */
	@Transactional
	public SalaCirurgicaResponse atualizar(Long id, SalaCirurgicaRequest request) {
		SalaCirurgica sala = buscar(id);
		exigirIdentificacaoLivre(request.identificacao().strip(), sala);
		mapper.atualizar(sala, request);
		return mapper.paraResponse(sala);
	}

	/**
	 * @throws RecursoNaoEncontradoException se a sala não existir
	 */
	public SalaCirurgicaResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	/**
	 * Lista as salas em ordem de identificação, todas ou só as ativas.
	 */
	public List<SalaCirurgicaResponse> listar(boolean somenteAtivas) {
		List<SalaCirurgica> salas = somenteAtivas
				? repository.findByAtivaTrueOrderByIdentificacaoAsc()
				: repository.findAll(Sort.by("identificacao"));
		return salas.stream().map(mapper::paraResponse).toList();
	}

	/**
	 * Inativa a sala, que deixa de receber agendamentos. Não é permitido enquanto houver procedimento
	 * agendado ou em andamento nela. Não tem efeito se ela já estiver inativa.
	 *
	 * @throws RecursoNaoEncontradoException se a sala não existir
	 * @throws RegraNegocioException se houver procedimento ativo na sala
	 */
	@Transactional
	public SalaCirurgicaResponse inativar(Long id) {
		SalaCirurgica sala = buscar(id);
		if (sala.isAtiva()) {
			if (procedimentoRepository.existeAtivoNaSala(id)) {
				throw new RegraNegocioException("A sala cirúrgica " + sala.getIdentificacao()
						+ " não pode ser inativada porque tem procedimento cirúrgico agendado ou em andamento.");
			}
			sala.inativar();
		}
		return mapper.paraResponse(sala);
	}

	/**
	 * Reativa a sala. Não tem efeito se ela já estiver ativa.
	 *
	 * @throws RecursoNaoEncontradoException se a sala não existir
	 */
	@Transactional
	public SalaCirurgicaResponse reativar(Long id) {
		SalaCirurgica sala = buscar(id);
		sala.reativar();
		return mapper.paraResponse(sala);
	}

	// A própria sala pode manter a sua identificação numa atualização; por isso a comparação é com ela.
	private void exigirIdentificacaoLivre(String identificacao, SalaCirurgica propria) {
		repository.findByIdentificacao(identificacao)
				.filter(existente -> existente != propria)
				.ifPresent(existente -> {
					throw new DadoJaCadastradoException(
							"Já existe uma sala cirúrgica com a identificação " + identificacao + ".");
				});
	}

	private SalaCirurgica buscar(Long id) {
		return Busca.salaCirurgica(repository, id);
	}

}
