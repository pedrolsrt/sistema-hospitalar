package br.pucminas.hospital.dto;

import java.time.LocalDateTime;

public record ProcedimentoCirurgicoResponse(
		Long id,
		PessoaResumo paciente,
		PessoaResumo profissional,
		String descricao,
		CodigoDescricao tipoCirurgia,
		LocalDateTime dataHoraPrevista,
		LocalDateTime dataHoraInicio,
		LocalDateTime dataHoraFim,
		CodigoDescricao situacao,
		String observacoesPreOperatorias,
		String observacoesPosOperatorias,
		String observacoes) implements AtendimentoResponse {
}
