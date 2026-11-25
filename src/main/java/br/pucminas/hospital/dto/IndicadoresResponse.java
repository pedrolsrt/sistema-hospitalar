package br.pucminas.hospital.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores gerenciais. O período, quando informado, vale para os indicadores de atendimentos; os quartos
 * mostram sempre a situação atual.
 *
 * @param inicio primeira data do período, ou nulo se não houver limite inicial
 * @param fim última data do período, incluída, ou nulo se não houver limite final
 * @param consultasRealizadas consultas concluídas no período
 * @param atendimentosPorModalidade todas as modalidades, inclusive as sem atendimentos
 * @param quartos quartos disponíveis e ocupados agora, sem relação com o período
 * @param atendimentosPorProfissional profissionais com atendimentos no período, do maior para o menor número
 */
public record IndicadoresResponse(
		LocalDate inicio,
		LocalDate fim,
		long consultasRealizadas,
		List<AtendimentosPorModalidadeResponse> atendimentosPorModalidade,
		QuartosPorSituacaoResponse quartos,
		List<AtendimentosPorProfissionalResponse> atendimentosPorProfissional) {
}
