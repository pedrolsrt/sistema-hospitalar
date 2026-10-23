package br.pucminas.hospital.model;

/**
 * Tipos de quarto e a capacidade máxima permitida em cada um. Os limites são uma suposição do grupo e
 * ficam só aqui (o banco não os repete) para que mudá-los não exija migration.
 */
public enum TipoQuarto {
	ENFERMARIA("Enfermaria", 6),
	APARTAMENTO("Apartamento", 2),
	UTI("UTI", 1);

	private final String descricao;
	private final int capacidadeMaximaPermitida;

	TipoQuarto(String descricao, int capacidadeMaximaPermitida) {
		this.descricao = descricao;
		this.capacidadeMaximaPermitida = capacidadeMaximaPermitida;
	}

	public boolean comporta(int capacidade) {
		return capacidade <= capacidadeMaximaPermitida;
	}

	public String getDescricao() {
		return descricao;
	}

	public int getCapacidadeMaximaPermitida() {
		return capacidadeMaximaPermitida;
	}

}
