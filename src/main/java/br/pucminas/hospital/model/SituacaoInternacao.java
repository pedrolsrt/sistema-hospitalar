package br.pucminas.hospital.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situações da internação. Não é gravada: é derivada da data efetiva de alta, e existe para que a
 * internação responda a {@link Atendimento#getSituacao()} como as demais modalidades.
 */
public enum SituacaoInternacao implements SituacaoAtendimento<SituacaoInternacao> {
	ATIVA("Ativa"),
	ENCERRADA("Encerrada");

	private final String descricao;

	SituacaoInternacao(String descricao) {
		this.descricao = descricao;
	}

	@Override
	public Set<SituacaoInternacao> getProximasPossiveis() {
		return switch (this) {
			case ATIVA -> EnumSet.of(ENCERRADA);
			case ENCERRADA -> EnumSet.noneOf(SituacaoInternacao.class);
		};
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

}
