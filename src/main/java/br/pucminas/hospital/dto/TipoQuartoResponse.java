package br.pucminas.hospital.dto;

/**
 * Tipo de quarto com o limite de leitos, para a tela mostrar o limite de cada tipo sem repetir a regra, que fica
 * só no enum TipoQuarto.
 */
public record TipoQuartoResponse(String codigo, String descricao, int capacidadeMaximaPermitida) {
}
