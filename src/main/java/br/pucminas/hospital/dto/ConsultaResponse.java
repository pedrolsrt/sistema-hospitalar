package br.pucminas.hospital.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Consulta de rotina ou especializada; a área de atuação só vem na especializada e a área de
 * encaminhamento só na rotina em que o paciente foi encaminhado.
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
		CodigoDescricao areaEncaminhamento) implements AtendimentoResponse {
}
