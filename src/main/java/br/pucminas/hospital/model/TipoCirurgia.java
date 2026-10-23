package br.pucminas.hospital.model;

public enum TipoCirurgia implements Descritivel {
	ELETIVA("Eletiva"),
	URGENCIA("Urgência"),
	EMERGENCIA("Emergência");

	private final String descricao;

	TipoCirurgia(String descricao) {
		this.descricao = descricao;
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

}
