package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.QuartoSemVagaException;
import br.pucminas.hospital.exception.RegraNegocioException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InternacaoTest {

	private static final LocalDate ENTRADA = LocalDate.of(2030, 3, 10);

	private final Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
	private final ProfissionalSaude medico = new ProfissionalSaude("Dr. João Souza", "CRM-MG 12345", "Clínica Geral");

	private Internacao internar(Quarto quarto) {
		return new Internacao(paciente, medico, quarto, ENTRADA, ENTRADA.plusDays(5));
	}

	@Test
	void novaInternacaoEstaAtivaEOcupaVagaDoQuarto() {
		Quarto quarto = new Quarto("201", 2, 2);

		Internacao internacao = internar(quarto);

		assertThat(internacao.isAtiva()).isTrue();
		assertThat(internacao.getDataEfetivaAlta()).isNull();
		assertThat(quarto.getOcupacaoAtual()).isEqualTo(1);
	}

	@Test
	void naoInternaEmQuartoSemVaga() {
		Quarto quarto = new Quarto("201", 2, 1);
		quarto.ocupar();

		assertThatThrownBy(() -> internar(quarto)).isInstanceOf(QuartoSemVagaException.class);
		assertThat(quarto.getOcupacaoAtual()).isEqualTo(1);
	}

	@Test
	void naoInternaComAltaPrevistaAnteriorAEntradaENaoOcupaVaga() {
		Quarto quarto = new Quarto("201", 2, 1);

		assertThatThrownBy(() -> new Internacao(paciente, medico, quarto, ENTRADA, ENTRADA.minusDays(1)))
				.isInstanceOf(DataInvalidaException.class)
				.hasMessage("A data de alta prevista (09/03/2030) não pode ser anterior à data de entrada (10/03/2030).");
		assertThat(quarto.getOcupacaoAtual()).isZero();
	}

	@Test
	void aceitaAltaPrevistaNoMesmoDiaDaEntrada() {
		Internacao internacao = new Internacao(paciente, medico, new Quarto("201", 2, 1), ENTRADA, ENTRADA);

		assertThat(internacao.getDataPrevistaAlta()).isEqualTo(ENTRADA);
	}

	@Test
	void darAltaEncerraInternacaoELiberaVaga() {
		Quarto quarto = new Quarto("201", 2, 1);
		Internacao internacao = internar(quarto);

		internacao.darAlta(ENTRADA.plusDays(3));

		assertThat(internacao.isAtiva()).isFalse();
		assertThat(internacao.getDataEfetivaAlta()).isEqualTo(ENTRADA.plusDays(3));
		assertThat(quarto.getOcupacaoAtual()).isZero();
		assertThat(quarto.getSituacao()).isEqualTo(SituacaoQuarto.DISPONIVEL);
	}

	@Test
	void darAltaNoDiaDaEntrada() {
		Internacao internacao = internar(new Quarto("201", 2, 1));

		internacao.darAlta(ENTRADA);

		assertThat(internacao.isAtiva()).isFalse();
	}

	@Test
	void naoDaAltaComDataAnteriorAEntrada() {
		Quarto quarto = new Quarto("201", 2, 1);
		Internacao internacao = internar(quarto);

		assertThatThrownBy(() -> internacao.darAlta(ENTRADA.minusDays(1)))
				.isInstanceOf(DataInvalidaException.class)
				.hasMessageContaining("alta efetiva");
		assertThat(internacao.isAtiva()).isTrue();
		assertThat(quarto.getOcupacaoAtual()).isEqualTo(1);
	}

	@Test
	void naoDaAltaDuasVezes() {
		Quarto quarto = new Quarto("201", 2, 2);
		Internacao internacao = internar(quarto);
		internacao.darAlta(ENTRADA.plusDays(2));

		assertThatThrownBy(() -> internacao.darAlta(ENTRADA.plusDays(4)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Esta internação já foi encerrada em 12/03/2030.");
		assertThat(internacao.getDataEfetivaAlta()).isEqualTo(ENTRADA.plusDays(2));
		assertThat(quarto.getOcupacaoAtual()).isZero();
	}

	@Test
	void alteraAltaPrevistaValida() {
		Internacao internacao = internar(new Quarto("201", 2, 1));

		internacao.setDataPrevistaAlta(ENTRADA.plusDays(10));

		assertThat(internacao.getDataPrevistaAlta()).isEqualTo(ENTRADA.plusDays(10));
	}

	@Test
	void naoAlteraAltaPrevistaParaAntesDaEntrada() {
		Internacao internacao = internar(new Quarto("201", 2, 1));

		assertThatThrownBy(() -> internacao.setDataPrevistaAlta(ENTRADA.minusDays(2)))
				.isInstanceOf(DataInvalidaException.class);
		assertThat(internacao.getDataPrevistaAlta()).isEqualTo(ENTRADA.plusDays(5));
	}

}
