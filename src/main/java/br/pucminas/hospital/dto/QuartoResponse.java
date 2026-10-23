package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.SituacaoQuarto;

public record QuartoResponse(
		Long id,
		String numero,
		int andar,
		CodigoDescricao tipo,
		int capacidadeMaxima,
		int ocupacaoAtual,
		SituacaoQuarto situacao) {
}
