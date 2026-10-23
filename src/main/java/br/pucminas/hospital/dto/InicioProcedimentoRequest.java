package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InicioProcedimentoRequest(
		@NotNull(message = "A data e hora de início é obrigatória.")
		LocalDateTime dataHoraInicio) {
}
