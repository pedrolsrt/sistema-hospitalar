package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ConclusaoProcedimentoRequest(
		@NotNull(message = "A data e hora de fim é obrigatória.")
		LocalDateTime dataHoraFim) {
}
