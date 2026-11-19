package br.pucminas.hospital.dto;

/**
 * Resposta de qualquer modalidade de atendimento; usada no histórico médico e na busca de atendimentos, que
 * reúnem todas.
 */
public sealed interface AtendimentoResponse permits ConsultaResponse, InternacaoResponse,
		AtendimentoEmergenciaResponse, ProcedimentoCirurgicoResponse {

	Long id();

	PessoaResumo paciente();

	PessoaResumo profissional();

}
