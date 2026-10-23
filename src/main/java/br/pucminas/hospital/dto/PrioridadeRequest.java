package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.PrioridadeEmergencia;
import jakarta.validation.constraints.NotNull;

public record PrioridadeRequest(
		@NotNull(message = "A prioridade é obrigatória.")
		PrioridadeEmergencia prioridade) {
}
