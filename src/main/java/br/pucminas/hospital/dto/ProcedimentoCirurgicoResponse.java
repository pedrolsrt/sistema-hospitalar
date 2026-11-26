package br.pucminas.hospital.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Procedimento cirúrgico. O período previsto vai da data e hora prevista até a data e hora de fim prevista,
 * calculada pela duração prevista.
 *
 * @param proximasSituacoes situações para as quais o procedimento pode passar agora, pela máquina de estados
 * @param permiteRegistrarObservacoesPreOperatorias se as observações pré-operatórias podem ser registradas agora
 * @param permiteRegistrarObservacoesPosOperatorias se as observações pós-operatórias podem ser registradas agora
 *        (procedimento em andamento ou concluído)
 */
public record ProcedimentoCirurgicoResponse(
		Long id,
		PessoaResumo paciente,
		PessoaResumo profissional,
		String descricao,
		CodigoDescricao tipoCirurgia,
		SalaCirurgicaResumo salaCirurgica,
		LocalDateTime dataHoraPrevista,
		int duracaoPrevistaMinutos,
		LocalDateTime dataHoraFimPrevista,
		LocalDateTime dataHoraInicio,
		LocalDateTime dataHoraFim,
		CodigoDescricao situacao,
		String observacoesPreOperatorias,
		String observacoesPosOperatorias,
		String observacoes,
		List<CodigoDescricao> proximasSituacoes,
		boolean permiteRegistrarObservacoesPreOperatorias,
		boolean permiteRegistrarObservacoesPosOperatorias) implements AtendimentoResponse {
}
