package br.pucminas.hospital.dto;

import java.time.LocalDateTime;

/**
 * Um atendimento no histórico: o momento que o situa, a modalidade e os dados próprios dela.
 */
public record HistoricoItemResponse(
		LocalDateTime dataHora,
		CodigoDescricao modalidade,
		AtendimentoResponse detalhes) {
}
