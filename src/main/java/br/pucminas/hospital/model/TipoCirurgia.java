package br.pucminas.hospital.model;

public enum TipoCirurgia {
	ELETIVA("Eletiva"),
	URGENCIA("Urgência"),
	EMERGENCIA("Emergência");

	private final String descricao;

	TipoCirurgia(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
