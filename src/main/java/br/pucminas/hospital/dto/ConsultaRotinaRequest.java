package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Agendamento de consulta de rotina. Que a data e o horário estejam no futuro é verificado pelo service,
 * que compara os dois juntos.
 */
public record ConsultaRotinaRequest(
		@NotNull(message = "O paciente é obrigatório.")
		Long pacienteId,

		@NotNull(message = "O profissional é obrigatório.")
		Long profissionalId,

		@NotNull(message = "A data da consulta é obrigatória.")
		LocalDate data,

		@NotNull(message = "O horário da consulta é obrigatório.")
		LocalTime horario,

		@NotBlank(message = "O motivo da consulta é obrigatório.")
		@Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres.")
		String motivo) {
}
