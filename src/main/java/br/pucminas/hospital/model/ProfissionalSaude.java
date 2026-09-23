package br.pucminas.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "profissional_saude")
public class ProfissionalSaude extends Pessoa {

	@Column(nullable = false, unique = true)
	private String registroProfissional;

	@Column(nullable = false)
	private String especialidade;

	protected ProfissionalSaude() {
	}

	public ProfissionalSaude(String nome, String registroProfissional, String especialidade) {
		super(nome);
		this.registroProfissional = registroProfissional;
		this.especialidade = especialidade;
	}

	public String getRegistroProfissional() {
		return registroProfissional;
	}

	public String getEspecialidade() {
		return especialidade;
	}

	public void setEspecialidade(String especialidade) {
		this.especialidade = especialidade;
	}

}
