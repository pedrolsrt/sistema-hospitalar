package br.pucminas.hospital.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Consulta de rotina ou especializada; a área de atuação só vem na especializada.
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
		String observacoes) implements AtendimentoResponse {
}
