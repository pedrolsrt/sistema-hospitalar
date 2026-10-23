package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SituacoesEmergenciaEProcedimentoTest {

	// Para cada par (atual, nova), se a transição é permitida segundo o mapa esperado.
	private static <S extends Enum<S>> Stream<Arguments> todosOsPares(S[] valores, Map<S, Set<S>> transicoesValidas) {
		return Arrays.stream(valores).flatMap(atual -> Arrays.stream(valores)
				.map(nova -> Arguments.of(atual, nova, transicoesValidas.get(atual).contains(nova))));
	}

	@Nested
	class SituacaoEmergenciaTest {

		static Stream<Arguments> pares() {
			return todosOsPares(SituacaoEmergencia.values(), Map.of(
					SituacaoEmergencia.AGUARDANDO_AVALIACAO, Set.of(SituacaoEmergencia.EM_ATENDIMENTO),
					SituacaoEmergencia.EM_ATENDIMENTO, Set.of(SituacaoEmergencia.CONCLUIDO),
					SituacaoEmergencia.CONCLUIDO, Set.of()));
		}

		@ParameterizedTest(name = "{0} -> {1}: {2}")
		@MethodSource("pares")
		void podeMudarParaSegueAMaquinaDeEstados(SituacaoEmergencia atual, SituacaoEmergencia nova, boolean permitida) {
			assertThat(atual.podeMudarPara(nova)).isEqualTo(permitida);
		}

		@Test
		void apenasConcluidoEFinal() {
			assertThat(Arrays.stream(SituacaoEmergencia.values()).filter(SituacaoEmergencia::isFinal))
					.containsExactly(SituacaoEmergencia.CONCLUIDO);
		}

	}

	@Nested
	class SituacaoProcedimentoTest {

		static Stream<Arguments> pares() {
			return todosOsPares(SituacaoProcedimento.values(), Map.of(
					SituacaoProcedimento.AGENDADO, Set.of(SituacaoProcedimento.EM_ANDAMENTO, SituacaoProcedimento.CANCELADO),
					SituacaoProcedimento.EM_ANDAMENTO, Set.of(SituacaoProcedimento.CONCLUIDO),
					SituacaoProcedimento.CONCLUIDO, Set.of(),
					SituacaoProcedimento.CANCELADO, Set.of()));
		}

		@ParameterizedTest(name = "{0} -> {1}: {2}")
		@MethodSource("pares")
		void podeMudarParaSegueAMaquinaDeEstados(SituacaoProcedimento atual, SituacaoProcedimento nova,
				boolean permitida) {
			assertThat(atual.podeMudarPara(nova)).isEqualTo(permitida);
		}

		@Test
		void agendadoNaoVaiDiretoParaConcluido() {
			assertThat(SituacaoProcedimento.AGENDADO.podeMudarPara(SituacaoProcedimento.CONCLUIDO)).isFalse();
		}

		@Test
		void situacoesFinais() {
			assertThat(Arrays.stream(SituacaoProcedimento.values()).filter(SituacaoProcedimento::isFinal))
					.containsExactlyInAnyOrder(SituacaoProcedimento.CONCLUIDO, SituacaoProcedimento.CANCELADO);
		}

		@Test
		void observacoesPosOperatoriasSoDepoisDoInicio() {
			assertThat(Arrays.stream(SituacaoProcedimento.values())
					.filter(SituacaoProcedimento::permiteObservacoesPosOperatorias))
					.containsExactlyInAnyOrder(SituacaoProcedimento.EM_ANDAMENTO, SituacaoProcedimento.CONCLUIDO);
		}

	}

	@Nested
	class PrioridadeEmergenciaTest {

		@Test
		void niveisCrescemDaBaixaParaACritica() {
			assertThat(PrioridadeEmergencia.BAIXA.getNivel()).isLessThan(PrioridadeEmergencia.MODERADA.getNivel());
			assertThat(PrioridadeEmergencia.MODERADA.getNivel()).isLessThan(PrioridadeEmergencia.ALTA.getNivel());
			assertThat(PrioridadeEmergencia.ALTA.getNivel()).isLessThan(PrioridadeEmergencia.CRITICA.getNivel());
		}

		@Test
		void todaPrioridadeTemDescricao() {
			assertThat(PrioridadeEmergencia.values()).allSatisfy(p -> assertThat(p.getDescricao()).isNotBlank());
		}

	}

}
