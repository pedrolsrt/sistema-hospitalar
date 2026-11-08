package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.AreaAtuacao;
import jakarta.validation.constraints.NotNull;

/**
 * Encaminhamento do paciente, feito em uma consulta de rotina, para atendimento especializado.
 */
public record EncaminhamentoRequest(
		@NotNull(message = "A área do atendimento especializado é obrigatória.")
		AreaAtuacao areaAtuacao) {
}
