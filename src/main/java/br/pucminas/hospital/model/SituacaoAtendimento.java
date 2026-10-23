package br.pucminas.hospital.model;

import java.util.Set;

/**
 * Situação de uma modalidade de atendimento que segue uma máquina de estados. Cada enum de situação
 * declara as transições permitidas; a verificação e a mensagem de erro ficam em {@link Atendimento}.
 */
public interface SituacaoAtendimento<S extends SituacaoAtendimento<S>> {

	Set<S> getProximasPossiveis();

	String getDescricao();

	default boolean podeMudarPara(S nova) {
		return getProximasPossiveis().contains(nova);
	}

	default boolean isFinal() {
		return getProximasPossiveis().isEmpty();
	}

}
