package br.pucminas.hospital.dto;

/**
 * Profissional de saúde. A área é a de atuação do especialista ou a cirúrgica do cirurgião, e é nula
 * para o clínico geral.
 */
public record ProfissionalSaudeResponse(
		Long id,
		String nome,
		String registroProfissional,
		CodigoDescricao categoria,
		CodigoDescricao areaAtuacao,
		String telefone,
		String email,
		boolean ativo) {
}
