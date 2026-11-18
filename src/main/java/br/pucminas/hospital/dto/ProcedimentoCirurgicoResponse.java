package br.pucminas.hospital.dto;

import java.time.LocalDateTime;

/**
 * Procedimento cirúrgico. O período previsto vai da data e hora prevista até a data e hora de fim prevista,
 * calculada pela duração prevista.
 */
public record ProcedimentoCirurgicoResponse(
		Long id,
		PessoaResumo paciente,
		PessoaResumo profissional,
		String descricao,
		CodigoDescricao tipoCirurgia,
		SalaCirurgicaResumo salaCirurgica,
		LocalDateTime dataHoraPrevista,
		int duracaoPrevistaMinutos,
		LocalDateTime dataHoraFimPrevista,
		LocalDateTime dataHoraInicio,
		LocalDateTime dataHoraFim,
		CodigoDescricao situacao,
		String observacoesPreOperatorias,
		String observacoesPosOperatorias,
		String observacoes) implements AtendimentoResponse {
}
