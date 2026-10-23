package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.AreaAtuacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Agendamento de consulta especializada: exige um médico especialista ativo da área informada.
 */
public record ConsultaEspecializadaRequest(
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
		String motivo,

		@NotNull(message = "A área de atuação da consulta especializada é obrigatória.")
		AreaAtuacao areaAtuacao) {
}
