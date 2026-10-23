package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Procedimento cirúrgico. As mudanças de situação seguem a máquina de estados de {@link SituacaoProcedimento}.
 */
@Entity
@Table(name = "procedimento_cirurgico")
public class ProcedimentoCirurgico extends Atendimento {

	private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	@Column(nullable = false)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoCirurgia tipoCirurgia;

	private String observacoesPreOperatorias;

	private String observacoesPosOperatorias;

	@Column(nullable = false)
	private LocalDateTime dataHoraPrevista;

	private LocalDateTime dataHoraInicio;

	private LocalDateTime dataHoraFim;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SituacaoProcedimento situacao;

	protected ProcedimentoCirurgico() {
	}

	/**
	 * Agenda o procedimento, que precisa poder ser realizado já no agendamento.
	 *
	 * @throws RegraNegocioException se o procedimento não puder ser realizado (ver {@link #validarCondicoesEspecificas()})
	 */
	public ProcedimentoCirurgico(Paciente paciente, ProfissionalSaude profissionalResponsavel, String descricao,
			TipoCirurgia tipoCirurgia, LocalDateTime dataHoraPrevista) {
		super(paciente, profissionalResponsavel);
		this.descricao = descricao;
		this.tipoCirurgia = tipoCirurgia;
		this.dataHoraPrevista = dataHoraPrevista;
		this.situacao = SituacaoProcedimento.AGENDADO;
		validarRealizacao();
	}

	/**
	 * Inicia o procedimento depois de verificar que ele pode ser realizado.
	 *
	 * @throws TransicaoSituacaoInvalidaException se o procedimento não estiver agendado
	 * @throws RegraNegocioException se o procedimento não puder ser realizado
	 */
	public void iniciar(LocalDateTime dataHoraInicio) {
		exigirTransicao(situacao, SituacaoProcedimento.EM_ANDAMENTO, "iniciar um procedimento cirúrgico");
		if (dataHoraInicio == null) {
			throw new RegraNegocioException("Informe a data e hora de início do procedimento.");
		}
		validarRealizacao();
		this.dataHoraInicio = dataHoraInicio;
		this.situacao = SituacaoProcedimento.EM_ANDAMENTO;
	}

	/**
	 * Conclui o procedimento. Só é possível depois de iniciado, e o fim não pode ser anterior ao início.
	 *
	 * @throws TransicaoSituacaoInvalidaException se o procedimento não estiver em andamento
	 * @throws DataInvalidaException se o fim for anterior ao início
	 */
	public void concluir(LocalDateTime dataHoraFim) {
		exigirTransicao(situacao, SituacaoProcedimento.CONCLUIDO, "concluir um procedimento cirúrgico");
		if (dataHoraFim == null) {
			throw new RegraNegocioException("Informe a data e hora de fim do procedimento.");
		}
		if (dataHoraFim.isBefore(dataHoraInicio)) {
			throw new DataInvalidaException("A data e hora de fim do procedimento (" + FORMATO_DATA_HORA.format(dataHoraFim)
					+ ") não pode ser anterior ao início (" + FORMATO_DATA_HORA.format(dataHoraInicio) + ").");
		}
		this.dataHoraFim = dataHoraFim;
		this.situacao = SituacaoProcedimento.CONCLUIDO;
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se o procedimento não estiver agendado
	 */
	public void cancelar() {
		exigirTransicao(situacao, SituacaoProcedimento.CANCELADO, "cancelar um procedimento cirúrgico");
		this.situacao = SituacaoProcedimento.CANCELADO;
	}

	public void registrarObservacoesPreOperatorias(String texto) {
		this.observacoesPreOperatorias = texto;
	}

	/**
	 * @throws RegraNegocioException se o procedimento ainda não tiver sido iniciado
	 */
	public void registrarObservacoesPosOperatorias(String texto) {
		if (!situacao.permiteObservacoesPosOperatorias()) {
			throw new RegraNegocioException("As observações pós-operatórias só podem ser registradas depois que o "
					+ "procedimento é iniciado. Situação atual: " + situacao.getDescricao() + ".");
		}
		this.observacoesPosOperatorias = texto;
	}

	/**
	 * Exige um profissional que possa realizar procedimentos (cirurgião), a descrição, o tipo de cirurgia
	 * e a data e hora prevista.
	 */
	@Override
	protected void validarCondicoesEspecificas() {
		ProfissionalSaude profissional = getProfissionalResponsavel();
		if (!profissional.podeRealizarProcedimentos()) {
			throw new RegraNegocioException("O profissional " + profissional.getNome()
					+ " não pode realizar procedimentos cirúrgicos: é necessário um cirurgião.");
		}
		if (descricao == null || descricao.isBlank()) {
			throw new RegraNegocioException("A descrição do procedimento cirúrgico é obrigatória.");
		}
		if (tipoCirurgia == null) {
			throw new RegraNegocioException("O tipo de cirurgia é obrigatório.");
		}
		if (dataHoraPrevista == null) {
			throw new RegraNegocioException("A data e hora prevista do procedimento são obrigatórias.");
		}
	}

	/**
	 * O início real quando o procedimento já começou; antes disso, a data e hora prevista.
	 */
	@Override
	public ModalidadeAtendimento getModalidade() {
		return ModalidadeAtendimento.PROCEDIMENTO_CIRURGICO;
	}

	@Override
	public <R> R aceitar(VisitanteAtendimento<R> visitante) {
		return visitante.visitar(this);
	}

	@Override
	public LocalDateTime getDataHoraReferencia() {
		return dataHoraInicio != null ? dataHoraInicio : dataHoraPrevista;
	}

	public String getDescricao() {
		return descricao;
	}

	public TipoCirurgia getTipoCirurgia() {
		return tipoCirurgia;
	}

	public String getObservacoesPreOperatorias() {
		return observacoesPreOperatorias;
	}

	public String getObservacoesPosOperatorias() {
		return observacoesPosOperatorias;
	}

	public LocalDateTime getDataHoraPrevista() {
		return dataHoraPrevista;
	}

	public LocalDateTime getDataHoraInicio() {
		return dataHoraInicio;
	}

	public LocalDateTime getDataHoraFim() {
		return dataHoraFim;
	}

	public SituacaoProcedimento getSituacao() {
		return situacao;
	}

}
