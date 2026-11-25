package br.pucminas.hospital.dto;

public record AtendimentosPorProfissionalResponse(
		Long profissionalId,
		String nome,
		CodigoDescricao categoria,
		long quantidade) {
}
