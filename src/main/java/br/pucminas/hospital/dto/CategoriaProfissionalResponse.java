package br.pucminas.hospital.dto;

/**
 * Categoria de profissional com a indicação de que exige área de atuação, para a tela pedir a área só quando
 * precisa sem repetir a regra, que fica só no enum CategoriaProfissional.
 */
public record CategoriaProfissionalResponse(String codigo, String descricao, boolean exigeArea) {
}
