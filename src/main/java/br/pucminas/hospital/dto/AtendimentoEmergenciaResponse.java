package br.pucminas.hospital.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Atendimento de emergência; a prioridade vem nula enquanto o paciente aguarda a triagem.
 *
 * @param proximasSituacoes situações para as quais o atendimento pode passar agora, pela máquina de estados
 * @param permiteIniciar se o atendimento pode ser iniciado agora (aguardando avaliação e com a prioridade definida)
 * @param permiteDefinirPrioridade se a prioridade pode ser definida ou reclassificada agora (até a conclusão)
 * @param permiteRegistrarObservacoes se as observações podem ser registradas agora (em atendimento ou concluído)
 */
public record AtendimentoEmergenciaResponse(
		Long id,
		PessoaResumo paciente,
		PessoaResumo profissional,
		CodigoDescricao prioridade,
		LocalDateTime dataHoraChegada,
		CodigoDescricao situacao,
		String observacoes,
		List<CodigoDescricao> proximasSituacoes,
		boolean permiteIniciar,
		boolean permiteDefinirPrioridade,
		boolean permiteRegistrarObservacoes) implements AtendimentoResponse {
}
