package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.TransicaoStatusInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "consulta")
public class Consulta extends Atendimento {

	@Column(nullable = false)
	private LocalDate data;

	@Column(nullable = false)
	private LocalTime horario;

	@Column(nullable = false)
	private String motivo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusConsulta status;

	protected Consulta() {
	}

	public Consulta(Paciente paciente, ProfissionalSaude profissionalResponsavel, LocalDate data, LocalTime horario,
			String motivo) {
		super(paciente, profissionalResponsavel);
		this.data = data;
		this.horario = horario;
		this.motivo = motivo;
		this.status = StatusConsulta.AGENDADA;
	}

	/**
	 * Marca a consulta como realizada.
	 *
	 * @throws TransicaoStatusInvalidaException se a consulta não estiver agendada
	 */
	public void realizar() {
		exigirAgendada("realizar");
		this.status = StatusConsulta.REALIZADA;
	}

	/**
	 * Cancela a consulta.
	 *
	 * @throws TransicaoStatusInvalidaException se a consulta não estiver agendada
	 */
	public void cancelar() {
		exigirAgendada("cancelar");
		this.status = StatusConsulta.CANCELADA;
	}

	private void exigirAgendada(String acao) {
		if (status != StatusConsulta.AGENDADA) {
			throw new TransicaoStatusInvalidaException(acao, status);
		}
	}

	public LocalDate getData() {
		return data;
	}

	public LocalTime getHorario() {
		return horario;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public StatusConsulta getStatus() {
		return status;
	}

}
