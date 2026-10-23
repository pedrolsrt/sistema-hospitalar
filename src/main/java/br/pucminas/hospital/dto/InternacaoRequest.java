package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record InternacaoRequest(
		@NotNull(message = "O paciente é obrigatório.")
		Long pacienteId,

		@NotNull(message = "O profissional responsável é obrigatório.")
		Long profissionalId,

		@NotNull(message = "O quarto é obrigatório.")
		Long quartoId,

		@NotNull(message = "A data de entrada é obrigatória.")
		LocalDate dataEntrada,

		@NotNull(message = "A data prevista de alta é obrigatória.")
		LocalDate dataPrevistaAlta) {
}
