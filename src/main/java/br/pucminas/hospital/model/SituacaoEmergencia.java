package br.pucminas.hospital.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situações do atendimento de emergência e as transições permitidas entre elas.
 */
public enum SituacaoEmergencia implements SituacaoAtendimento<SituacaoEmergencia> {
	AGUARDANDO_AVALIACAO("Aguardando avaliação"),
	EM_ATENDIMENTO("Em atendimento"),
	CONCLUIDO("Concluído");

	private final String descricao;

	SituacaoEmergencia(String descricao) {
		this.descricao = descricao;
	}

	@Override
	public Set<SituacaoEmergencia> getProximasPossiveis() {
		return switch (this) {
			case AGUARDANDO_AVALIACAO -> EnumSet.of(EM_ATENDIMENTO);
			case EM_ATENDIMENTO -> EnumSet.of(CONCLUIDO);
			case CONCLUIDO -> EnumSet.noneOf(SituacaoEmergencia.class);
		};
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

}
