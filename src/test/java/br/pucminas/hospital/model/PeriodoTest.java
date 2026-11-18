package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PeriodoTest {

	private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

	// Período fixo das 14:00 às 15:00, comparado com outros de mesmo tamanho ou não.
	private static final Periodo DAS_14_AS_15 = new Periodo(DIA.atTime(14, 0), DIA.atTime(15, 0));

	private static Periodo periodo(String inicio, String fim) {
		return new Periodo(DIA.atTime(LocalTime.parse(inicio)), DIA.atTime(LocalTime.parse(fim)));
	}

	@ParameterizedTest(name = "{0} às {1}: sobrepõe = {2}")
	@CsvSource({
			"13:00, 14:00, false", // termina quando o outro começa
			"15:00, 16:00, false", // começa quando o outro termina
			"12:00, 13:00, false",
			"13:00, 14:01, true",
			"14:59, 16:00, true",
			"14:15, 14:45, true", // contido no outro
			"13:00, 16:00, true", // contém o outro
			"14:00, 15:00, true" // igual
	})
	void encostarNoInicioOuNoFimNaoESobreposicao(String inicio, String fim, boolean sobrepoe) {
		Periodo outro = periodo(inicio, fim);

		assertThat(DAS_14_AS_15.sobrepoe(outro)).isEqualTo(sobrepoe);
		assertThat(outro.sobrepoe(DAS_14_AS_15)).isEqualTo(sobrepoe);
	}

	@Test
	void criaPelaDuracao() {
		Periodo periodo = Periodo.de(DIA.atTime(23, 30), Duration.ofMinutes(45));

		assertThat(periodo.fim()).isEqualTo(DIA.plusDays(1).atTime(0, 15));
	}

	@Test
	void naoAceitaFimAnteriorOuIgualAoInicio() {
		LocalDateTime inicio = DIA.atTime(14, 0);

		assertThatThrownBy(() -> new Periodo(inicio, inicio)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Periodo(inicio, inicio.minusMinutes(1)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Periodo(null, inicio)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void descricaoNoMesmoDiaENaViradaDaMeiaNoite() {
		assertThat(DAS_14_AS_15.descricao()).isEqualTo("15/01/2030, das 14:00 às 15:00");
		assertThat(new Periodo(DIA.atTime(23, 50), DIA.plusDays(1).atTime(0, 20)).descricao())
				.isEqualTo("15/01/2030 às 23:50 até 16/01/2030 às 00:20");
	}

}
