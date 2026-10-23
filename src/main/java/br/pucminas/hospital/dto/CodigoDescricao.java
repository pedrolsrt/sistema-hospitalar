package br.pucminas.hospital.dto;

/**
 * Valor de um enum na resposta: o código, que o cliente envia de volta nas requisições, e a descrição
 * legível para exibir.
 */
public record CodigoDescricao(String codigo, String descricao) {
}
