package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.TipoQuarto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cadastro e atualização de quarto. O limite de capacidade de cada tipo é regra de negócio,
 * verificada pelo domínio.
 */
public record QuartoRequest(
		@NotBlank(message = "O número do quarto é obrigatório.")
		@Size(max = 10, message = "O número do quarto deve ter no máximo 10 caracteres.")
		String numero,

		@NotNull(message = "O andar é obrigatório.")
		Integer andar,

		@NotNull(message = "O tipo do quarto é obrigatório.")
		TipoQuarto tipo,

		@NotNull(message = "A capacidade máxima é obrigatória.")
		@Positive(message = "A capacidade máxima deve ser maior que zero.")
		Integer capacidadeMaxima) {
}
