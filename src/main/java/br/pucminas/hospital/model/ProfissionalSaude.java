package br.pucminas.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Profissional de saúde. O que muda entre as categorias fica em métodos sobrescritos pelas subclasses,
 * para que as regras perguntem ao profissional o que ele pode fazer em vez de testar o seu tipo.
 */
@Entity
@Table(name = "profissional_saude")
// As categorias diferem só por uma coluna de área; com tabela única, listar profissionais e carregar o
// responsável de um atendimento não exige joins.
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 30)
public abstract class ProfissionalSaude extends Pessoa {

	@Column(nullable = false, unique = true)
	private String registroProfissional;

	protected ProfissionalSaude() {
	}

	protected ProfissionalSaude(String nome, String registroProfissional) {
		super(nome);
		this.registroProfissional = registroProfissional;
	}

	/**
	 * Indica se o profissional atua na área informada. Por padrão, nenhuma.
	 */
	public boolean atendeArea(AreaAtuacao area) {
		return false;
	}

	/**
	 * Indica se o profissional pode ser responsável por procedimentos cirúrgicos. Por padrão, não.
	 */
	public boolean podeRealizarProcedimentos() {
		return false;
	}

	public String getRegistroProfissional() {
		return registroProfissional;
	}

}
