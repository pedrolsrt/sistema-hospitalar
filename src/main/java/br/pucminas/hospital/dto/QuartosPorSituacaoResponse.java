package br.pucminas.hospital.dto;

/**
 * Quartos pela situação atual: disponível enquanto há vaga, ocupado quando a capacidade foi atingida.
 */
public record QuartosPorSituacaoResponse(long disponiveis, long ocupados) {
}
