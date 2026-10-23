package br.pucminas.hospital.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situação de uma modalidade de atendimento que segue uma máquina de estados. Cada enum de situação
 * declara as transições permitidas; a verificação e a mensagem de erro ficam em {@link Atendimento}.
 */
public interface SituacaoAtendimento<S extends SituacaoAtendimento<S>> extends Descritivel {

	/**
	 * Situações ainda não finais: o atendimento está em curso ou por acontecer.
	 */
	static <S extends Enum<S> & SituacaoAtendimento<S>> Set<S> ativas(Class<S> tipo) {
		EnumSet<S> ativas = EnumSet.noneOf(tipo);
		for (S situacao : tipo.getEnumConstants()) {
			if (!situacao.isFinal()) {
				ativas.add(situacao);
			}
		}
		return ativas;
	}

	Set<S> getProximasPossiveis();

	default boolean podeMudarPara(S nova) {
		return getProximasPossiveis().contains(nova);
	}

	default boolean isFinal() {
		return getProximasPossiveis().isEmpty();
	}

}
