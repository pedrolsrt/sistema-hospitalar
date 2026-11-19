package br.pucminas.hospital.dto;

import java.time.LocalDateTime;

/**
 * Atendimento de qualquer modalidade numa lista (histórico médico e busca de atendimentos): o momento que o
 * situa, a modalidade, a situação e os dados próprios da modalidade.
 */
public record AtendimentoItemResponse(
		LocalDateTime dataHora,
		CodigoDescricao modalidade,
		CodigoDescricao situacao,
		AtendimentoResponse detalhes) {
}
