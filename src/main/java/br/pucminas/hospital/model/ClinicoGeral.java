package br.pucminas.hospital.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CLINICO_GERAL")
public class ClinicoGeral extends ProfissionalSaude {

	protected ClinicoGeral() {
	}

	public ClinicoGeral(String nome, String registroProfissional) {
		super(nome, registroProfissional);
	}

}
