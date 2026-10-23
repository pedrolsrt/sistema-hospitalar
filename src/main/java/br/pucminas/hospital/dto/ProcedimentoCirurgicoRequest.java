package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.TipoCirurgia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ProcedimentoCirurgicoRequest(
		@NotNull(message = "O paciente é obrigatório.")
		Long pacienteId,

		@NotNull(message = "O cirurgião é obrigatório.")
		Long cirurgiaoId,

		@NotBlank(message = "A descrição do procedimento é obrigatória.")
		@Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
		String descricao,

		@NotNull(message = "O tipo de cirurgia é obrigatório.")
		TipoCirurgia tipoCirurgia,

		@NotNull(message = "A data e hora prevista é obrigatória.")
		LocalDateTime dataHoraPrevista,

		@Size(max = 1000, message = "As observações pré-operatórias devem ter no máximo 1000 caracteres.")
		String observacoesPreOperatorias) {
}
