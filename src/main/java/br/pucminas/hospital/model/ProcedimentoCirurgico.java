package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.ProfissionalIncompativelException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Procedimento cirúrgico. As mudanças de situação seguem a máquina de estados de {@link SituacaoProcedimento}.
 */
@Entity
@Table(name = "procedimento_cirurgico")
public class ProcedimentoCirurgico extends Atendimento {

	public static final int DURACAO_MINIMA_MINUTOS = 15;
	public static final int DURACAO_MAXIMA_MINUTOS = 720;

	private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	@Column(nullable = false)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoCirurgia tipoCirurgia;

	private String observacoesPreOperatorias;

	private String observacoesPosOperatorias;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sala_cirurgica_id", nullable = false)
	private SalaCirurgica salaCirurgica;

	@Column(nullable = false)
	private LocalDateTime dataHoraPrevista;

	@Column(name = "duracao_prevista_minutos", nullable = false)
	private int duracaoPrevistaMinutos;

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
			TipoCirurgia tipoCirurgia, SalaCirurgica salaCirurgica, LocalDateTime dataHoraPrevista,
			int duracaoPrevistaMinutos) {
		super(paciente, profissionalResponsavel);
		this.descricao = descricao;
		this.tipoCirurgia = tipoCirurgia;
		this.salaCirurgica = salaCirurgica;
		this.dataHoraPrevista = dataHoraPrevista;
		this.duracaoPrevistaMinutos = duracaoPrevistaMinutos;
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
	 * Exige um profissional que possa realizar procedimentos (cirurgião), a descrição, o tipo de cirurgia,
	 * uma sala cirúrgica ativa, a data e hora prevista e a duração prevista dentro dos limites.
	 *
	 * @throws ProfissionalIncompativelException se o profissional não puder realizar procedimentos
	 * @throws RegraNegocioException se faltar algum dado, a sala estiver inativa ou a duração estiver fora
	 *         dos limites
	 */
	@Override
	protected void validarCondicoesEspecificas() {
		ProfissionalSaude profissional = getProfissionalResponsavel();
		if (!profissional.podeRealizarProcedimentos()) {
			throw new ProfissionalIncompativelException("O profissional " + profissional.getNome()
					+ " não pode realizar procedimentos cirúrgicos: é necessário um cirurgião.");
		}
		if (descricao == null || descricao.isBlank()) {
			throw new RegraNegocioException("A descrição do procedimento cirúrgico é obrigatória.");
		}
		if (tipoCirurgia == null) {
			throw new RegraNegocioException("O tipo de cirurgia é obrigatório.");
		}
		if (salaCirurgica == null) {
			throw new RegraNegocioException("A sala cirúrgica do procedimento é obrigatória.");
		}
		if (!salaCirurgica.isAtiva()) {
			throw new RegraNegocioException("A sala cirúrgica " + salaCirurgica.getIdentificacao()
					+ " está inativa e não pode receber procedimentos.");
		}
		if (dataHoraPrevista == null) {
			throw new RegraNegocioException("A data e hora prevista do procedimento são obrigatórias.");
		}
		if (duracaoPrevistaMinutos < DURACAO_MINIMA_MINUTOS || duracaoPrevistaMinutos > DURACAO_MAXIMA_MINUTOS) {
			throw new RegraNegocioException("A duração prevista do procedimento deve ser de " + DURACAO_MINIMA_MINUTOS
					+ " a " + DURACAO_MAXIMA_MINUTOS + " minutos; a duração informada foi de " + duracaoPrevistaMinutos
					+ " minutos.");
		}
	}

	/**
	 * Da data e hora prevista até o fim da duração prevista. É o período usado na verificação de
	 * disponibilidade, mesmo depois de o procedimento ter começado.
	 */
	public Periodo getPeriodoPrevisto() {
		return Periodo.de(dataHoraPrevista, Duration.ofMinutes(duracaoPrevistaMinutos));
	}

	@Override
	public ModalidadeAtendimento getModalidade() {
		return ModalidadeAtendimento.PROCEDIMENTO_CIRURGICO;
	}

	@Override
	public <R> R aceitar(VisitanteAtendimento<R> visitante) {
		return visitante.visitar(this);
	}

	/**
	 * O início real quando o procedimento já começou; antes disso, a data e hora prevista.
	 */
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

	public SalaCirurgica getSalaCirurgica() {
		return salaCirurgica;
	}

	public LocalDateTime getDataHoraPrevista() {
		return dataHoraPrevista;
	}

	public int getDuracaoPrevistaMinutos() {
		return duracaoPrevistaMinutos;
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
