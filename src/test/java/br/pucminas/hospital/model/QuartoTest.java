package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.QuartoSemVagaException;
import br.pucminas.hospital.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class QuartoTest {

	@Test
	void novoQuartoComecaVazioEDisponivel() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 2);

		assertThat(quarto.getOcupacaoAtual()).isZero();
		assertThat(quarto.temVaga()).isTrue();
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.DISPONIVEL);
	}

	@Test
	void naoCriaQuartoComCapacidadeZeroOuNegativa() {
		assertThatThrownBy(() -> new Quarto("101", 1, TipoQuarto.ENFERMARIA, 0))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("maior que zero");
		assertThatThrownBy(() -> new Quarto("101", 1, TipoQuarto.ENFERMARIA, -1))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void ocuparIncrementaOcupacaoEFicaOcupadoAoAtingirCapacidade() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 2);

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
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 1);
		quarto.ocupar();

		assertThatThrownBy(quarto::ocupar)
				.isInstanceOf(QuartoSemVagaException.class)
				.hasMessage("O quarto 101 não possui vagas disponíveis.");
		assertThat(quarto.getOcupacaoAtual()).isEqualTo(1);
	}

	@Test
	void liberarDecrementaOcupacaoETornaQuartoDisponivel() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 1);
		quarto.ocupar();

		quarto.liberar();

		assertThat(quarto.getOcupacaoAtual()).isZero();
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.DISPONIVEL);
	}

	@Test
	void liberarQuartoVazioLancaExcecao() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 1);

		assertThatThrownBy(quarto::liberar)
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("não possui pacientes internados");
		assertThat(quarto.getOcupacaoAtual()).isZero();
	}

	@Test
	void alteraCapacidadeQuandoComportaOcupacaoAtual() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 3);
		quarto.ocupar();
		quarto.ocupar();

		quarto.setCapacidadeMaxima(2);

		assertThat(quarto.getCapacidadeMaxima()).isEqualTo(2);
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.OCUPADO);
	}

	@Test
	void naoReduzCapacidadeAbaixoDaOcupacaoAtual() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 3);
		quarto.ocupar();
		quarto.ocupar();

		assertThatThrownBy(() -> quarto.setCapacidadeMaxima(1))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("menor que a ocupação atual");
		assertThat(quarto.getCapacidadeMaxima()).isEqualTo(3);
	}

	@ParameterizedTest
	@CsvSource({ "ENFERMARIA, 6", "APARTAMENTO, 2", "UTI, 1" })
	void limiteDeCapacidadeDeCadaTipo(TipoQuarto tipo, int limite) {
		assertThat(tipo.getCapacidadeMaximaPermitida()).isEqualTo(limite);
	}

	@ParameterizedTest
	@EnumSource(TipoQuarto.class)
	void criaQuartoNoLimiteDoTipo(TipoQuarto tipo) {
		Quarto quarto = new Quarto("101", 1, tipo, tipo.getCapacidadeMaximaPermitida());

		assertThat(quarto.getTipo()).isEqualTo(tipo);
		assertThat(quarto.getCapacidadeMaxima()).isEqualTo(tipo.getCapacidadeMaximaPermitida());
	}

	@ParameterizedTest
	@EnumSource(TipoQuarto.class)
	void naoCriaQuartoAcimaDoLimiteDoTipo(TipoQuarto tipo) {
		int acimaDoLimite = tipo.getCapacidadeMaximaPermitida() + 1;

		assertThatThrownBy(() -> new Quarto("101", 1, tipo, acimaDoLimite))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Um quarto do tipo " + tipo.getDescricao() + " comporta no máximo "
						+ tipo.getCapacidadeMaximaPermitida() + " leito(s); a capacidade informada foi " + acimaDoLimite
						+ ".");
	}

	@Test
	void exigeTipo() {
		assertThatThrownBy(() -> new Quarto("101", 1, null, 1))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O tipo do quarto é obrigatório.");
	}

	@Test
	void naoAumentaCapacidadeAcimaDoLimiteDoTipo() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.APARTAMENTO, 1);

		assertThatThrownBy(() -> quarto.setCapacidadeMaxima(3)).isInstanceOf(RegraNegocioException.class);
		assertThat(quarto.getCapacidadeMaxima()).isEqualTo(1);
	}

	@Test
	void alteraTipoQuandoComportaACapacidade() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 1);

		quarto.setTipo(TipoQuarto.UTI);

		assertThat(quarto.getTipo()).isEqualTo(TipoQuarto.UTI);
	}

	@Test
	void naoAlteraTipoQueNaoComportaACapacidade() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 4);

		assertThatThrownBy(() -> quarto.setTipo(TipoQuarto.APARTAMENTO)).isInstanceOf(RegraNegocioException.class);
		assertThat(quarto.getTipo()).isEqualTo(TipoQuarto.ENFERMARIA);
	}

}
