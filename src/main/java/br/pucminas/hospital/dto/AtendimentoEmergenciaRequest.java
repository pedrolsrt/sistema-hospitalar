package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.PrioridadeEmergencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;

/**
 * Registro da chegada à emergência. A prioridade pode ficar para a avaliação, e a chegada, se omitida,
 * é o momento do registro.
 */
public record AtendimentoEmergenciaRequest(
		@NotNull(message = "O paciente é obrigatório.")
		Long pacienteId,

		@NotNull(message = "O profissional de plantão é obrigatório.")
		Long profissionalId,

		PrioridadeEmergencia prioridade,

		@PastOrPresent(message = "A data e hora de chegada não pode estar no futuro.")
		LocalDateTime dataHoraChegada) {
}
