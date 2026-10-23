package br.pucminas.hospital.model;

import static br.pucminas.hospital.model.SituacaoConsulta.AGENDADA;
import static br.pucminas.hospital.model.SituacaoConsulta.CANCELADA;
import static br.pucminas.hospital.model.SituacaoConsulta.CONCLUIDA;
import static br.pucminas.hospital.model.SituacaoConsulta.CONFIRMADA;
import static br.pucminas.hospital.model.SituacaoConsulta.EM_ATENDIMENTO;
import static br.pucminas.hospital.model.SituacaoConsulta.NAO_COMPARECEU;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class SituacaoConsultaTest {

	// Transições do CLAUDE.md, escritas de forma independente da implementação do enum.
	private static final Map<SituacaoConsulta, Set<SituacaoConsulta>> TRANSICOES_VALIDAS = Map.of(
			AGENDADA, Set.of(CONFIRMADA, CANCELADA, NAO_COMPARECEU),
			CONFIRMADA, Set.of(EM_ATENDIMENTO, CANCELADA, NAO_COMPARECEU),
			EM_ATENDIMENTO, Set.of(CONCLUIDA),
			CONCLUIDA, Set.of(),
			CANCELADA, Set.of(),
			NAO_COMPARECEU, Set.of());

	static Stream<Arguments> todosOsPares() {
		return Arrays.stream(SituacaoConsulta.values())
				.flatMap(atual -> Arrays.stream(SituacaoConsulta.values())
						.map(nova -> Arguments.of(atual, nova, TRANSICOES_VALIDAS.get(atual).contains(nova))));
	}

	@ParameterizedTest(name = "{0} -> {1}: {2}")
	@MethodSource("todosOsPares")
	void podeMudarParaSegueAMaquinaDeEstados(SituacaoConsulta atual, SituacaoConsulta nova, boolean permitida) {
		assertThat(atual.podeMudarPara(nova)).isEqualTo(permitida);
	}

	@ParameterizedTest
	@EnumSource(SituacaoConsulta.class)
	void nenhumaSituacaoMudaParaSiMesma(SituacaoConsulta situacao) {
		assertThat(situacao.podeMudarPara(situacao)).isFalse();
	}

	@Test
	void situacoesFinais() {
		assertThat(situacoesQue(SituacaoConsulta::isFinal)).containsExactlyInAnyOrder(CONCLUIDA, CANCELADA, NAO_COMPARECEU);
	}

	@Test
	void situacoesAtivasOcupamHorario() {
		assertThat(situacoesQue(SituacaoConsulta::isAtiva)).containsExactlyInAnyOrder(AGENDADA, CONFIRMADA, EM_ATENDIMENTO);
	}

	@Test
	void observacoesSoDuranteOuDepoisDoAtendimento() {
		assertThat(situacoesQue(SituacaoConsulta::permiteRegistrarObservacoes))
				.containsExactlyInAnyOrder(EM_ATENDIMENTO, CONCLUIDA);
	}

	@ParameterizedTest
	@EnumSource(SituacaoConsulta.class)
	void todaSituacaoTemDescricaoParaExibicao(SituacaoConsulta situacao) {
		assertThat(situacao.getDescricao()).isNotBlank();
	}

	private static Stream<SituacaoConsulta> situacoesQue(Predicate<SituacaoConsulta> criterio) {
		return Arrays.stream(SituacaoConsulta.values()).filter(criterio);
	}

}
