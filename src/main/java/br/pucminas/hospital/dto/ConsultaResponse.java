package br.pucminas.hospital.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Consulta de rotina ou especializada; a área de atuação só vem na especializada e a área de
 * encaminhamento só na rotina em que o paciente foi encaminhado.
 *
 * @param proximasSituacoes situações para as quais a consulta pode passar agora, pela máquina de estados
 * @param permiteRegistrarObservacoes se as observações podem ser registradas na situação atual
 * @param permiteEncaminhar se o paciente pode ser encaminhado agora (consulta de rotina com clínico geral, em
 *        atendimento ou concluída)
 */
public record ConsultaResponse(
		Long id,
		CodigoDescricao modalidade,
		PessoaResumo paciente,
		PessoaResumo profissional,
		LocalDate data,
		LocalTime horario,
		String motivo,
		CodigoDescricao areaAtuacao,
		CodigoDescricao situacao,
		String observacoes,
		CodigoDescricao areaEncaminhamento,
		List<CodigoDescricao> proximasSituacoes,
		boolean permiteRegistrarObservacoes,
		boolean permiteEncaminhar) implements AtendimentoResponse {
}
