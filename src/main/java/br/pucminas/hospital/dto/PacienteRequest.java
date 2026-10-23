package br.pucminas.hospital.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Cadastro de paciente. O CPF é aceito com ou sem pontuação e gravado só com os dígitos.
 */
public record PacienteRequest(
		@NotBlank(message = "O nome é obrigatório.")
		@Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
		String nome,

		@NotBlank(message = "O CPF é obrigatório.")
		@CPF(message = "O CPF informado não é válido.")
		String cpf,

		@NotNull(message = "A data de nascimento é obrigatória.")
		@Past(message = "A data de nascimento deve estar no passado.")
		LocalDate dataNascimento,

		@Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
		String telefone,

		@Email(message = "O e-mail informado não é válido.")
		@Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
		String email,

		@Valid
		EnderecoDto endereco) {
}
