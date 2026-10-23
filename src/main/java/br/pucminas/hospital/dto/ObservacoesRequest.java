package br.pucminas.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Texto de observações de um atendimento (gerais, pré-operatórias ou pós-operatórias).
 */
public record ObservacoesRequest(
		@NotBlank(message = "O texto das observações é obrigatório.")
		@Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
		String texto) {
}
