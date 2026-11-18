package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro e atualização de sala cirúrgica.
 */
public record SalaCirurgicaRequest(
		@NotBlank(message = "A identificação da sala é obrigatória.")
		@Size(max = 30, message = "A identificação da sala deve ter no máximo 30 caracteres.")
		String identificacao) {
}
