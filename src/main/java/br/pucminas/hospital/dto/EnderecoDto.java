package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Endereço do paciente, usado na requisição e na resposta. O CEP é aceito com ou sem hífen.
 */
public record EnderecoDto(
		@NotBlank(message = "O logradouro é obrigatório.")
		@Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres.")
		String logradouro,

		@Size(max = 10, message = "O número deve ter no máximo 10 caracteres.")
		String numero,

		@Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres.")
		String complemento,

		@Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres.")
		String bairro,

		@NotBlank(message = "A cidade é obrigatória.")
		@Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
		String cidade,

		@NotBlank(message = "A UF é obrigatória.")
		@Pattern(regexp = "[A-Za-z]{2}", message = "A UF deve ter duas letras.")
		String uf,

		@Pattern(regexp = "\\d{5}-?\\d{3}", message = "O CEP deve ter 8 dígitos, com ou sem hífen.")
		String cep) {
}
