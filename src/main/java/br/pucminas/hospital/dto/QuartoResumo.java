package br.pucminas.hospital.dto;

/**
 * Quarto referenciado em uma internação.
 */
public record QuartoResumo(Long id, String numero, CodigoDescricao tipo) {
}
