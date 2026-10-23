package br.pucminas.hospital.model;

/**
 * Operação que depende da modalidade do atendimento (padrão Visitor). Cada modalidade chama o método
 * correspondente em {@link Atendimento#aceitar(VisitanteAtendimento)}, o que dispensa testes de tipo e
 * funciona também quando o atendimento é um proxy do Hibernate.
 */
public interface VisitanteAtendimento<R> {

	R visitar(Consulta consulta);

	R visitar(Internacao internacao);

	R visitar(AtendimentoEmergencia emergencia);

	R visitar(ProcedimentoCirurgico procedimento);

}
