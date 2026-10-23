package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.AltaRequest;
import br.pucminas.hospital.dto.InternacaoRequest;
import br.pucminas.hospital.dto.InternacaoResponse;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.QuartoSemVagaException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.InternacaoMapper;
import br.pucminas.hospital.model.Internacao;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.repository.InternacaoRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import br.pucminas.hospital.repository.QuartoRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internações e altas. A ocupação do quarto é controlada pela própria internação (ocupa na admissão,
 * libera na alta); o @Version do quarto impede que duas internações simultâneas ultrapassem a capacidade.
 */
@Service
@Transactional(readOnly = true)
public class InternacaoService {

	private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final InternacaoRepository repository;
	private final PacienteRepository pacienteRepository;
	private final ProfissionalSaudeRepository profissionalRepository;
	private final QuartoRepository quartoRepository;
	private final InternacaoMapper mapper;

	public InternacaoService(InternacaoRepository repository, PacienteRepository pacienteRepository,
			ProfissionalSaudeRepository profissionalRepository, QuartoRepository quartoRepository,
			InternacaoMapper mapper) {
		this.repository = repository;
		this.pacienteRepository = pacienteRepository;
		this.profissionalRepository = profissionalRepository;
		this.quartoRepository = quartoRepository;
		this.mapper = mapper;
	}

	/**
	 * Interna o paciente, ocupando uma vaga do quarto.
	 *
	 * @throws RegraNegocioException se o paciente já estiver internado ou o paciente ou o profissional
	 *         estiver inativo
	 * @throws QuartoSemVagaException se o quarto não tiver vaga
	 * @throws DataInvalidaException se a alta prevista for anterior à entrada
	 */
	@Transactional
	public InternacaoResponse internar(InternacaoRequest request) {
		Paciente paciente = Busca.paciente(pacienteRepository, request.pacienteId());
		ProfissionalSaude profissional = Busca.profissional(profissionalRepository, request.profissionalId());
		Quarto quarto = Busca.quarto(quartoRepository, request.quartoId());
		repository.findByPacienteIdAndDataEfetivaAltaIsNull(paciente.getId()).ifPresent(ativa -> {
			throw new RegraNegocioException("O paciente " + paciente.getNome() + " já está internado no quarto "
					+ ativa.getQuarto().getNumero() + " desde " + FORMATO_DATA.format(ativa.getDataEntrada()) + ".");
		});
		Internacao internacao = new Internacao(paciente, profissional, quarto, request.dataEntrada(),
				request.dataPrevistaAlta());
		return mapper.paraResponse(repository.save(internacao));
	}

	/**
	 * Registra a alta e libera a vaga do quarto.
	 *
	 * @throws RegraNegocioException se a internação já estiver encerrada
	 * @throws DataInvalidaException se a data de alta for anterior à entrada
	 */
	@Transactional
	public InternacaoResponse registrarAlta(Long id, AltaRequest request) {
		Internacao internacao = buscar(id);
		internacao.darAlta(request.dataEfetivaAlta());
		return mapper.paraResponse(internacao);
	}

	/**
	 * @throws RecursoNaoEncontradoException se a internação não existir
	 */
	public InternacaoResponse buscarPorId(Long id) {
		return mapper.paraResponse(buscar(id));
	}

	/**
	 * Internações sem alta, em ordem de entrada.
	 */
	public List<InternacaoResponse> listarAtivas() {
		return repository.findByDataEfetivaAltaIsNullOrderByDataEntradaAsc().stream().map(mapper::paraResponse).toList();
	}

	/**
	 * Internações com alta, da alta mais recente para a mais antiga.
	 */
	public List<InternacaoResponse> listarEncerradas() {
		return repository.findByDataEfetivaAltaIsNotNullOrderByDataEfetivaAltaDesc().stream()
				.map(mapper::paraResponse).toList();
	}

	private Internacao buscar(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Internação não encontrada (id " + id + ")."));
	}

}
