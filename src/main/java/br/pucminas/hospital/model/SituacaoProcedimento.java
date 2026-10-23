package br.pucminas.hospital.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situações do procedimento cirúrgico e as transições permitidas entre elas. Só se chega a CONCLUIDO
 * a partir de EM_ANDAMENTO, o que impede concluir um procedimento que não foi iniciado.
 */
public enum SituacaoProcedimento implements SituacaoAtendimento<SituacaoProcedimento> {
	AGENDADO("Agendado"),
	EM_ANDAMENTO("Em andamento"),
	CONCLUIDO("Concluído"),
	CANCELADO("Cancelado");

	private final String descricao;

	SituacaoProcedimento(String descricao) {
		this.descricao = descricao;
	}

	@Override
	public Set<SituacaoProcedimento> getProximasPossiveis() {
		return switch (this) {
			case AGENDADO -> EnumSet.of(EM_ANDAMENTO, CANCELADO);
			case EM_ANDAMENTO -> EnumSet.of(CONCLUIDO);
			case CONCLUIDO, CANCELADO -> EnumSet.noneOf(SituacaoProcedimento.class);
		};
	}

	public boolean permiteObservacoesPosOperatorias() {
		return this == EM_ANDAMENTO || this == CONCLUIDO;
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

}
