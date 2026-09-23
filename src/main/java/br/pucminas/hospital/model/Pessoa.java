package br.pucminas.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.Hibernate;

@MappedSuperclass
public abstract class Pessoa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	private String telefone;

	private String email;

	@Column(nullable = false)
	private boolean ativo = true;

	protected Pessoa() {
	}

	protected Pessoa(String nome) {
		this.nome = nome;
	}

	/**
	 * Pessoas nunca são excluídas fisicamente, para preservar o histórico; apenas deixam de estar ativas.
	 */
	public void inativar() {
		this.ativo = false;
	}

	public void reativar() {
		this.ativo = true;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isAtivo() {
		return ativo;
	}

	// Compara pela classe real (e não por instanceof) para que Paciente 1 e ProfissionalSaude 1 sejam
	// diferentes, e usa Hibernate.getClass para que um proxy seja igual à entidade que representa.
	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}
		Pessoa outra = (Pessoa) o;
		return id != null && id.equals(outra.getId());
	}

	// Constante por classe: o hash não muda quando o id é gerado ao persistir.
	@Override
	public final int hashCode() {
		return Hibernate.getClass(this).hashCode();
	}

}
