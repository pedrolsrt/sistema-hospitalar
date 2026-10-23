package br.pucminas.hospital.dto;

import java.time.LocalDateTime;

public record AtendimentoEmergenciaResponse(
		Long id,
		PessoaResumo paciente,
		PessoaResumo profissional,
		CodigoDescricao prioridade,
		LocalDateTime dataHoraChegada,
		CodigoDescricao situacao,
		String observacoes) implements AtendimentoResponse {
}
