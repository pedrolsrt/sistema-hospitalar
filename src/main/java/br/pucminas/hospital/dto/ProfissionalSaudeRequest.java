package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.CategoriaProfissional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de profissional de saúde. A área de atuação é obrigatória para médico especialista
 * (área de atuação) e cirurgião (área cirúrgica), e não deve ser informada para clínico geral.
 */
@AreaCompativelComCategoria
public record ProfissionalSaudeRequest(
		@NotBlank(message = "O nome é obrigatório.")
		@Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
		String nome,

		@NotBlank(message = "O registro profissional é obrigatório.")
		@Size(max = 30, message = "O registro profissional deve ter no máximo 30 caracteres.")
		String registroProfissional,

		@NotNull(message = "A categoria é obrigatória.")
		CategoriaProfissional categoria,

		AreaAtuacao areaAtuacao,

		@Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
		String telefone,

		@Email(message = "O e-mail informado não é válido.")
		@Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
		String email) {
}
