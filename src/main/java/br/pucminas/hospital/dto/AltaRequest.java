package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record AltaRequest(
		@NotNull(message = "A data efetiva de alta é obrigatória.")
		@PastOrPresent(message = "A data efetiva de alta não pode estar no futuro.")
		LocalDate dataEfetivaAlta) {
}
