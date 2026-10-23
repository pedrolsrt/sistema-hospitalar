package br.pucminas.hospital.dto;

import java.time.LocalDate;

public record InternacaoResponse(
		Long id,
		PessoaResumo paciente,
		PessoaResumo profissional,
		QuartoResumo quarto,
		LocalDate dataEntrada,
		LocalDate dataPrevistaAlta,
		LocalDate dataEfetivaAlta,
		boolean ativa,
		String observacoes) implements AtendimentoResponse {
}
