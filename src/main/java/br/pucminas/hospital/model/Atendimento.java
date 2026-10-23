package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.RegraNegocioException;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;

/**
 * Atendimento a um paciente. A validação de realização segue o padrão Template Method: as verificações
 * comuns ficam em {@link #validarRealizacao()} e cada modalidade acrescenta as suas em
 * {@link #validarCondicoesEspecificas()}.
 */
@Entity
@Table(name = "atendimento")
// JOINED: as modalidades têm campos bem diferentes entre si, e cada tabela mantém as suas colunas
// obrigatórias como NOT NULL, o que a tabela única não permitiria.
@Inheritance(strategy = InheritanceType.JOINED)
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

	/**
	 * Verifica se o atendimento pode ser realizado: paciente e profissional ativos e, em seguida,
	 * as condições específicas da modalidade.
	 *
	 * @throws RegraNegocioException se alguma condição não for atendida
	 */
	public final void validarRealizacao() {
		if (!paciente.isAtivo()) {
			throw new RegraNegocioException(
					"O paciente " + paciente.getNome() + " está inativo e não pode ser atendido.");
		}
		if (!profissionalResponsavel.isAtivo()) {
			throw new RegraNegocioException("O profissional " + profissionalResponsavel.getNome()
					+ " está inativo e não pode realizar atendimentos.");
		}
		validarCondicoesEspecificas();
	}

	/**
	 * Condições próprias da modalidade, verificadas depois das comuns.
	 *
	 * @throws RegraNegocioException se alguma condição não for atendida
	 */
	protected abstract void validarCondicoesEspecificas();

	/**
	 * Registra as observações do atendimento. Modalidades que restringem quando isso é possível sobrescrevem.
	 */
	public void registrarObservacoes(String texto) {
		this.observacoes = texto;
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
