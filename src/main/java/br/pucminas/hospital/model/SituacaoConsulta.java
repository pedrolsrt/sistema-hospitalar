package br.pucminas.hospital.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situações da consulta e as transições permitidas entre elas. Remarcar uma consulta cancelada
 * não é uma transição: cria-se uma nova consulta.
 */
public enum SituacaoConsulta implements SituacaoAtendimento<SituacaoConsulta> {
	AGENDADA("Agendada", true),
	CONFIRMADA("Confirmada", true),
	EM_ATENDIMENTO("Em atendimento", true),
	CONCLUIDA("Concluída", false),
	CANCELADA("Cancelada", false),
	NAO_COMPARECEU("Não compareceu", false);

	private final String descricao;

	// Consultas ativas ocupam o horário do profissional e do paciente na verificação de sobreposição.
	private final boolean ativa;

	SituacaoConsulta(String descricao, boolean ativa) {
		this.descricao = descricao;
		this.ativa = ativa;
	}

	// Um switch, e não um campo preenchido no construtor, porque uma constante não pode
	// referenciar as que são declaradas depois dela.
	@Override
	public Set<SituacaoConsulta> getProximasPossiveis() {
		return switch (this) {
			case AGENDADA -> EnumSet.of(CONFIRMADA, CANCELADA, NAO_COMPARECEU);
			case CONFIRMADA -> EnumSet.of(EM_ATENDIMENTO, CANCELADA, NAO_COMPARECEU);
			case EM_ATENDIMENTO -> EnumSet.of(CONCLUIDA);
			case CONCLUIDA, CANCELADA, NAO_COMPARECEU -> EnumSet.noneOf(SituacaoConsulta.class);
		};
	}

	public boolean isAtiva() {
		return ativa;
	}

	public boolean permiteRegistrarObservacoes() {
		return this == EM_ATENDIMENTO || this == CONCLUIDA;
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

}
