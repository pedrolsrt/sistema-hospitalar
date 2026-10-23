package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.QuartoRequest;
import br.pucminas.hospital.dto.QuartoResponse;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.QuartoMapper;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
import br.pucminas.hospital.repository.InternacaoRepository;
import br.pucminas.hospital.repository.QuartoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de quartos. O limite de capacidade de cada tipo é verificado pelo próprio Quarto.
 */
@Service
@Transactional(readOnly = true)
public class QuartoService {

	private final QuartoRepository repository;
	private final InternacaoRepository internacaoRepository;
	private final QuartoMapper mapper;

	public QuartoService(QuartoRepository repository, InternacaoRepository internacaoRepository, QuartoMapper mapper) {
		this.repository = repository;
		this.internacaoRepository = internacaoRepository;
		this.mapper = mapper;
	}

	/**
	 * Cadastra um quarto vazio.
	 *
	 * @throws DadoJaCadastradoException se já houver quarto com o mesmo número
	 * @throws RegraNegocioException se a capacidade ultrapassar o limite do tipo
	 */
	@Transactional
	public QuartoResponse cadastrar(QuartoRequest request) {
		Quarto quarto = mapper.paraEntidade(request);
		exigirNumeroLivre(quarto.getNumero(), null);
		return mapper.paraResponse(repository.save(quarto));
	}

	/**
	 * Atualiza número, andar, tipo e capacidade.
	 *
	 * @throws RecursoNaoEncontradoException se o quarto não existir
	 * @throws DadoJaCadastradoException se o novo número já pertencer a outro quarto
	 * @throws RegraNegocioException se a capacidade ultrapassar o limite do tipo ou ficar abaixo da ocupação
	 */
	@Transactional
	public QuartoResponse atualizar(Long id, QuartoRequest request) {
		Quarto quarto = buscar(id);
		exigirNumeroLivre(request.numero().strip(), quarto);
		mapper.atualizar(quarto, request);
		return mapper.paraResponse(quarto);
	}

	/**
	 * @throws RecursoNaoEncontradoException se o quarto não existir
	 */
	public QuartoResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	public List<QuartoResponse> listar() {
		return paraResponses(repository.findAll(Sort.by("numero")));
	}

	public List<QuartoResponse> listarComVaga() {
		return paraResponses(repository.findComVaga());
	}

	public List<QuartoResponse> listarPorTipo(TipoQuarto tipo) {
		return paraResponses(repository.findByTipoOrderByNumeroAsc(tipo));
	}

	/**
	 * Remove o quarto. Não é permitido se ele tiver internação ativa nem se já tiver recebido
	 * internações, cujo histórico depende dele.
	 *
	 * @throws RecursoNaoEncontradoException se o quarto não existir
	 * @throws RegraNegocioException se o quarto tiver internação ativa ou histórico de internações
	 */
	@Transactional
	public void remover(Long id) {
		Quarto quarto = buscar(id);
		if (internacaoRepository.existsByQuartoIdAndDataEfetivaAltaIsNull(id)) {
			throw new RegraNegocioException(
					"O quarto " + quarto.getNumero() + " tem internação ativa e não pode ser removido.");
		}
		if (internacaoRepository.existsByQuartoId(id)) {
			throw new RegraNegocioException("O quarto " + quarto.getNumero()
					+ " tem histórico de internações e não pode ser removido, para preservar o histórico dos pacientes.");
		}
		repository.delete(quarto);
	}

	// O próprio quarto pode manter o seu número numa atualização; por isso a comparação é com ele.
	private void exigirNumeroLivre(String numero, Quarto proprio) {
		repository.findByNumero(numero)
				.filter(existente -> existente != proprio)
				.ifPresent(existente -> {
					throw new DadoJaCadastradoException("Já existe um quarto com o número " + numero + ".");
				});
	}

	private Quarto buscar(Long id) {
		return Busca.quarto(repository, id);
	}

	private List<QuartoResponse> paraResponses(List<Quarto> quartos) {
		return quartos.stream().map(mapper::paraResponse).toList();
	}

}
