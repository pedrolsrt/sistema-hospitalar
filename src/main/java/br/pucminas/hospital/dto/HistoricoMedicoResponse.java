package br.pucminas.hospital.dto;

import java.util.List;

/**
 * Histórico médico do paciente: todos os atendimentos, de todas as modalidades, em ordem cronológica.
 */
public record HistoricoMedicoResponse(
		PessoaResumo paciente,
		List<HistoricoItemResponse> atendimentos) {
}
