package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Consulta agendada. As mudanças de situação seguem a máquina de estados de {@link SituacaoConsulta}.
 */
@Entity
@Table(name = "consulta")
public abstract class Consulta extends Atendimento {

	@Column(nullable = false)
	private LocalDate data;

	@Column(nullable = false)
	private LocalTime horario;

	@Column(nullable = false)
	private String motivo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SituacaoConsulta situacao;

	protected Consulta() {
	}

	protected Consulta(Paciente paciente, ProfissionalSaude profissionalResponsavel, LocalDate data,
			LocalTime horario, String motivo) {
		super(paciente, profissionalResponsavel);
		this.data = data;
		this.horario = horario;
		this.motivo = motivo;
		this.situacao = SituacaoConsulta.AGENDADA;
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver agendada
	 */
	public void confirmar() {
		mudarPara(SituacaoConsulta.CONFIRMADA, "confirmar uma consulta");
	}

	/**
	 * Inicia o atendimento depois de verificar que ele pode ser realizado.
	 *
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver confirmada
	 * @throws RegraNegocioException se o atendimento não puder ser realizado
	 */
	public void iniciarAtendimento() {
		exigirTransicao(situacao, SituacaoConsulta.EM_ATENDIMENTO, "iniciar o atendimento de uma consulta");
		validarRealizacao();
		situacao = SituacaoConsulta.EM_ATENDIMENTO;
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver em atendimento
	 */
	public void concluir() {
		mudarPara(SituacaoConsulta.CONCLUIDA, "concluir uma consulta");
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver agendada ou confirmada
	 */
	public void cancelar() {
		mudarPara(SituacaoConsulta.CANCELADA, "cancelar uma consulta");
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se a consulta não estiver agendada ou confirmada
	 */
	public void registrarNaoComparecimento() {
		mudarPara(SituacaoConsulta.NAO_COMPARECEU, "registrar o não comparecimento de uma consulta");
	}

	/**
	 * Registra as observações, permitido apenas durante o atendimento ou depois da conclusão.
	 *
	 * @throws RegraNegocioException se a consulta estiver em outra situação
	 */
	@Override
	public void registrarObservacoes(String texto) {
		if (!situacao.permiteRegistrarObservacoes()) {
			throw new RegraNegocioException("As observações só podem ser registradas durante o atendimento "
					+ "ou depois da conclusão da consulta. Situação atual: " + situacao.getDescricao() + ".");
		}
		super.registrarObservacoes(texto);
	}

	private void mudarPara(SituacaoConsulta nova, String acao) {
		exigirTransicao(situacao, nova, acao);
		situacao = nova;
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

	public SituacaoConsulta getSituacao() {
		return situacao;
	}

}
