package br.pucminas.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;

/**
 * Sala onde os procedimentos cirúrgicos são realizados. Salas não são excluídas, para preservar o
 * histórico dos procedimentos; deixam de receber novos agendamentos ao serem inativadas.
 */
@Entity
@Table(name = "sala_cirurgica")
public class SalaCirurgica {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String identificacao;

	@Column(nullable = false)
	private boolean ativa = true;

	protected SalaCirurgica() {
	}

	public SalaCirurgica(String identificacao) {
		this.identificacao = identificacao;
	}

	public void inativar() {
		this.ativa = false;
	}

	public void reativar() {
		this.ativa = true;
	}

	public Long getId() {
		return id;
	}

	public String getIdentificacao() {
		return identificacao;
	}

	public void setIdentificacao(String identificacao) {
		this.identificacao = identificacao;
	}

	public boolean isAtiva() {
		return ativa;
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}
		SalaCirurgica outra = (SalaCirurgica) o;
		return id != null && id.equals(outra.getId());
	}

	@Override
	public final int hashCode() {
		return Hibernate.getClass(this).hashCode();
	}

}
