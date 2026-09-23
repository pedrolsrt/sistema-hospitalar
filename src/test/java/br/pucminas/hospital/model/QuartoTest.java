package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.QuartoSemVagaException;
import br.pucminas.hospital.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

class QuartoTest {

	@Test
	void novoQuartoComecaVazioEDisponivel() {
		Quarto quarto = new Quarto("101", 1, 2);

		assertThat(quarto.getOcupacaoAtual()).isZero();
		assertThat(quarto.temVaga()).isTrue();
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.DISPONIVEL);
	}

	@Test
	void naoCriaQuartoComCapacidadeZeroOuNegativa() {
		assertThatThrownBy(() -> new Quarto("101", 1, 0))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("maior que zero");
		assertThatThrownBy(() -> new Quarto("101", 1, -1))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void ocuparIncrementaOcupacaoEFicaOcupadoAoAtingirCapacidade() {
		Quarto quarto = new Quarto("101", 1, 2);

		quarto.ocupar();
		assertThat(quarto.getOcupacaoAtual()).isEqualTo(1);
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.DISPONIVEL);

		quarto.ocupar();
		assertThat(quarto.getOcupacaoAtual()).isEqualTo(2);
		assertThat(quarto.temVaga()).isFalse();
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.OCUPADO);
	}

	@Test
	void ocuparQuartoLotadoLancaExcecaoSemAlterarOcupacao() {
		Quarto quarto = new Quarto("101", 1, 1);
		quarto.ocupar();

		assertThatThrownBy(quarto::ocupar)
				.isInstanceOf(QuartoSemVagaException.class)
				.hasMessage("O quarto 101 não possui vagas disponíveis.");
		assertThat(quarto.getOcupacaoAtual()).isEqualTo(1);
	}

	@Test
	void liberarDecrementaOcupacaoETornaQuartoDisponivel() {
		Quarto quarto = new Quarto("101", 1, 1);
		quarto.ocupar();

		quarto.liberar();

		assertThat(quarto.getOcupacaoAtual()).isZero();
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.DISPONIVEL);
	}

	@Test
	void liberarQuartoVazioLancaExcecao() {
		Quarto quarto = new Quarto("101", 1, 1);

		assertThatThrownBy(quarto::liberar)
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("não possui pacientes internados");
		assertThat(quarto.getOcupacaoAtual()).isZero();
	}

	@Test
	void alteraCapacidadeQuandoComportaOcupacaoAtual() {
		Quarto quarto = new Quarto("101", 1, 3);
		quarto.ocupar();
		quarto.ocupar();

		quarto.setCapacidadeMaxima(2);

		assertThat(quarto.getCapacidadeMaxima()).isEqualTo(2);
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.OCUPADO);
	}

	@Test
	void naoReduzCapacidadeAbaixoDaOcupacaoAtual() {
		Quarto quarto = new Quarto("101", 1, 3);
		quarto.ocupar();
		quarto.ocupar();

		assertThatThrownBy(() -> quarto.setCapacidadeMaxima(1))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("menor que a ocupação atual");
		assertThat(quarto.getCapacidadeMaxima()).isEqualTo(3);
	}

}
