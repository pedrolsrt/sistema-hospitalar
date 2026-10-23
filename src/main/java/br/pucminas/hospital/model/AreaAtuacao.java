package br.pucminas.hospital.model;

public enum AreaAtuacao {
	CARDIOLOGIA("Cardiologia"),
	NEUROLOGIA("Neurologia"),
	ORTOPEDIA("Ortopedia"),
	PEDIATRIA("Pediatria"),
	DERMATOLOGIA("Dermatologia"),
	GINECOLOGIA("Ginecologia"),
	OBSTETRICIA("Obstetrícia"),
	OFTALMOLOGIA("Oftalmologia"),
	OTORRINOLARINGOLOGIA("Otorrinolaringologia"),
	PSIQUIATRIA("Psiquiatria"),
	ENDOCRINOLOGIA("Endocrinologia"),
	GASTROENTEROLOGIA("Gastroenterologia"),
	PNEUMOLOGIA("Pneumologia"),
	NEFROLOGIA("Nefrologia"),
	UROLOGIA("Urologia"),
	ONCOLOGIA("Oncologia"),
	CIRURGIA_GERAL("Cirurgia Geral"),
	CIRURGIA_CARDIOVASCULAR("Cirurgia Cardiovascular"),
	NEUROCIRURGIA("Neurocirurgia");

	private final String descricao;

	AreaAtuacao(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
