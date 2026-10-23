package br.pucminas.hospital.model;

public enum PrioridadeEmergencia implements Descritivel {
	BAIXA(1, "Baixa"),
	MODERADA(2, "Moderada"),
	ALTA(3, "Alta"),
	CRITICA(4, "Crítica");

	// Nível explícito, e não a ordem de declaração, para que reordenar as constantes não mude a fila.
	private final int nivel;
	private final String descricao;

	PrioridadeEmergencia(int nivel, String descricao) {
		this.nivel = nivel;
		this.descricao = descricao;
	}

	public int getNivel() {
		return nivel;
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

}
