package br.pucminas.hospital.model;

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.Hibernate;

@MappedSuperclass
public abstract class Atendimento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "paciente_id", nullable = false)
	private Paciente paciente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profissional_responsavel_id", nullable = false)
	private ProfissionalSaude profissionalResponsavel;

	private String observacoes;

	protected Atendimento() {
	}

	protected Atendimento(Paciente paciente, ProfissionalSaude profissionalResponsavel) {
		this.paciente = paciente;
		this.profissionalResponsavel = profissionalResponsavel;
	}

	public Long getId() {
		return id;
	}

	public Paciente getPaciente() {
		return paciente;
	}

	public ProfissionalSaude getProfissionalResponsavel() {
		return profissionalResponsavel;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}
		Atendimento outro = (Atendimento) o;
		return id != null && id.equals(outro.getId());
	}

	@Override
	public final int hashCode() {
		return Hibernate.getClass(this).hashCode();
	}

}
