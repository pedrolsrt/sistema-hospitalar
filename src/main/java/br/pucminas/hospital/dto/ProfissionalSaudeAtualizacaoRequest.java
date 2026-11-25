package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Atualização dos dados comuns do profissional. Registro, categoria e área não mudam por aqui.
 */
public record ProfissionalSaudeAtualizacaoRequest(
		@NotBlank(message = "O nome é obrigatório.")
		@Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
		String nome,

		@Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
		String telefone,

		@EmailValido
		@Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
		String email) {
}
