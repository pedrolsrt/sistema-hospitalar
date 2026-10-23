package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.QuartoRequest;
import br.pucminas.hospital.dto.QuartoResponse;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.QuartoMapper;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.SituacaoQuarto;
import br.pucminas.hospital.model.TipoQuarto;
import br.pucminas.hospital.repository.InternacaoRepository;
import br.pucminas.hospital.repository.QuartoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class QuartoServiceTest {

	@Mock
	private QuartoRepository repository;

	@Mock
	private InternacaoRepository internacaoRepository;

	private QuartoService service;

	@BeforeEach
	void criarService() {
		service = new QuartoService(repository, internacaoRepository, new QuartoMapper());
	}

	@Test
	void cadastraQuartoVazio() {
		when(repository.findByNumero("101")).thenReturn(Optional.empty());
		when(repository.save(any(Quarto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		QuartoResponse resposta = service.cadastrar(new QuartoRequest(" 101 ", 1, TipoQuarto.APARTAMENTO, 2));

		assertThat(resposta.numero()).isEqualTo("101");
		assertThat(resposta.tipo().descricao()).isEqualTo("Apartamento");
		assertThat(resposta.ocupacaoAtual()).isZero();
		assertThat(resposta.situacao()).isEqualTo(SituacaoQuarto.DISPONIVEL);
	}

	@Test
	void naoCadastraNumeroJaExistente() {
		when(repository.findByNumero("101")).thenReturn(Optional.of(new Quarto("101", 1, TipoQuarto.UTI, 1)));

		assertThatThrownBy(() -> service.cadastrar(new QuartoRequest("101", 2, TipoQuarto.ENFERMARIA, 4)))
				.isInstanceOf(DadoJaCadastradoException.class)
				.hasMessage("Já existe um quarto com o número 101.");
		verify(repository, never()).save(any());
	}

	@Test
	void naoCadastraCapacidadeAcimaDoLimiteDoTipo() {
		assertThatThrownBy(() -> service.cadastrar(new QuartoRequest("201", 2, TipoQuarto.UTI, 2)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Um quarto do tipo UTI comporta no máximo 1 leito(s); a capacidade informada foi 2.");
		verify(repository, never()).save(any());
	}

	@Test
	void atualizaTipoECapacidadeMantendoONumero() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.APARTAMENTO, 2);
		when(repository.findById(1L)).thenReturn(Optional.of(quarto));
		when(repository.findByNumero("101")).thenReturn(Optional.of(quarto));

		QuartoResponse resposta = service.atualizar(1L, new QuartoRequest("101", 3, TipoQuarto.ENFERMARIA, 5));

		assertThat(quarto.getTipo()).isEqualTo(TipoQuarto.ENFERMARIA);
		assertThat(quarto.getCapacidadeMaxima()).isEqualTo(5);
		assertThat(resposta.andar()).isEqualTo(3);
	}

	@Test
	void atualizaParaUmNumeroLivre() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.UTI, 1);
		when(repository.findById(1L)).thenReturn(Optional.of(quarto));
		when(repository.findByNumero("102")).thenReturn(Optional.empty());

		service.atualizar(1L, new QuartoRequest("102", 1, TipoQuarto.UTI, 1));

		assertThat(quarto.getNumero()).isEqualTo("102");
	}

	@Test
	void naoAtualizaParaONumeroDeOutroQuarto() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.UTI, 1);
		when(repository.findById(1L)).thenReturn(Optional.of(quarto));
		when(repository.findByNumero("102")).thenReturn(Optional.of(new Quarto("102", 1, TipoQuarto.UTI, 1)));

		assertThatThrownBy(() -> service.atualizar(1L, new QuartoRequest("102", 1, TipoQuarto.UTI, 1)))
				.isInstanceOf(DadoJaCadastradoException.class);
		assertThat(quarto.getNumero()).isEqualTo("101");
	}

	@Test
	void naoReduzCapacidadeAbaixoDaOcupacao() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 4);
		quarto.ocupar();
		quarto.ocupar();
		when(repository.findById(1L)).thenReturn(Optional.of(quarto));
		when(repository.findByNumero("101")).thenReturn(Optional.of(quarto));

		assertThatThrownBy(() -> service.atualizar(1L, new QuartoRequest("101", 1, TipoQuarto.UTI, 1)))
				.isInstanceOf(RegraNegocioException.class);
		assertThat(quarto.getCapacidadeMaxima()).isEqualTo(4);
	}

	@Test
	void naoAtualizaQuartoInexistente() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.atualizar(9L, new QuartoRequest("101", 1, TipoQuarto.UTI, 1)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Quarto não encontrado (id 9).");
	}

	@Test
	void buscaPorIdELista() {
		Quarto uti = new Quarto("201", 2, TipoQuarto.UTI, 1);
		Quarto enfermaria = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 6);
		when(repository.findById(1L)).thenReturn(Optional.of(uti));
		when(repository.findAll(Sort.by("numero"))).thenReturn(List.of(enfermaria, uti));
		when(repository.findComVaga()).thenReturn(List.of(enfermaria));
		when(repository.findByTipoOrderByNumeroAsc(TipoQuarto.UTI)).thenReturn(List.of(uti));

		assertThat(service.buscarPorId(1L).numero()).isEqualTo("201");
		assertThat(service.listar()).extracting(QuartoResponse::numero).containsExactly("101", "201");
		assertThat(service.listarComVaga()).extracting(QuartoResponse::numero).containsExactly("101");
		assertThat(service.listarPorTipo(TipoQuarto.UTI)).extracting(QuartoResponse::numero).containsExactly("201");
	}

	@Test
	void removeQuartoQueNuncaRecebeuInternacao() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.UTI, 1);
		when(repository.findById(1L)).thenReturn(Optional.of(quarto));
		when(internacaoRepository.existsByQuartoIdAndDataEfetivaAltaIsNull(1L)).thenReturn(false);
		when(internacaoRepository.existsByQuartoId(1L)).thenReturn(false);

		service.remover(1L);

		verify(repository).delete(quarto);
	}

	@Test
	void naoRemoveQuartoComInternacaoAtiva() {
		when(repository.findById(1L)).thenReturn(Optional.of(new Quarto("101", 1, TipoQuarto.UTI, 1)));
		when(internacaoRepository.existsByQuartoIdAndDataEfetivaAltaIsNull(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.remover(1L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O quarto 101 tem internação ativa e não pode ser removido.");
		verify(repository, never()).delete(any());
	}

	@Test
	void naoRemoveQuartoComHistoricoDeInternacoes() {
		when(repository.findById(1L)).thenReturn(Optional.of(new Quarto("101", 1, TipoQuarto.UTI, 1)));
		when(internacaoRepository.existsByQuartoIdAndDataEfetivaAltaIsNull(1L)).thenReturn(false);
		when(internacaoRepository.existsByQuartoId(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.remover(1L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("histórico de internações");
		verify(repository, never()).delete(any());
	}

	@Test
	void naoRemoveQuartoInexistente() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.remover(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
		verify(repository, never()).delete(any());
	}

}
