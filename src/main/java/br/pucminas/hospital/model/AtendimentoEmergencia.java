package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Atendimento de emergência, registrado na chegada do paciente com o profissional de plantão.
 * A prioridade pode ficar indefinida até a avaliação, mas é exigida para iniciar o atendimento.
 */
@Entity
@Table(name = "atendimento_emergencia")
public class AtendimentoEmergencia extends Atendimento {

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private PrioridadeEmergencia prioridade;

	@Column(nullable = false)
	private LocalDateTime dataHoraChegada;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SituacaoEmergencia situacao;

	protected AtendimentoEmergencia() {
	}

	/**
	 * @param prioridade pode ser nula quando o paciente ainda não foi avaliado
	 * @throws RegraNegocioException se a data e hora de chegada não forem informadas
	 */
	public AtendimentoEmergencia(Paciente paciente, ProfissionalSaude profissionalPlantao,
			PrioridadeEmergencia prioridade, LocalDateTime dataHoraChegada) {
		super(paciente, profissionalPlantao);
		if (dataHoraChegada == null) {
			throw new RegraNegocioException("A data e hora de chegada do paciente são obrigatórias.");
		}
		this.prioridade = prioridade;
		this.dataHoraChegada = dataHoraChegada;
		this.situacao = SituacaoEmergencia.AGUARDANDO_AVALIACAO;
	}

	/**
	 * Define ou reclassifica a prioridade enquanto o atendimento não foi concluído.
	 *
	 * @throws RegraNegocioException se a prioridade não for informada ou o atendimento já estiver concluído
	 */
	public void definirPrioridade(PrioridadeEmergencia prioridade) {
		if (prioridade == null) {
			throw new RegraNegocioException("Informe a prioridade do atendimento de emergência.");
		}
		if (situacao.isFinal()) {
			throw new RegraNegocioException(
					"Não é possível alterar a prioridade de um atendimento de emergência já concluído.");
		}
		this.prioridade = prioridade;
	}

	/**
	 * Inicia o atendimento depois de verificar que ele pode ser realizado.
	 *
	 * @throws TransicaoSituacaoInvalidaException se o atendimento não estiver aguardando avaliação
	 * @throws RegraNegocioException se o atendimento não puder ser realizado
	 */
	public void iniciar() {
		exigirTransicao(situacao, SituacaoEmergencia.EM_ATENDIMENTO, "iniciar um atendimento de emergência");
		validarRealizacao();
		situacao = SituacaoEmergencia.EM_ATENDIMENTO;
	}

	/**
	 * @throws TransicaoSituacaoInvalidaException se o atendimento não estiver em andamento
	 */
	public void concluir() {
		exigirTransicao(situacao, SituacaoEmergencia.CONCLUIDO, "concluir um atendimento de emergência");
		situacao = SituacaoEmergencia.CONCLUIDO;
	}

	/**
	 * Exige a prioridade definida.
	 */
	@Override
	protected void validarCondicoesEspecificas() {
		if (prioridade == null) {
			throw new RegraNegocioException(
					"Defina a prioridade do atendimento de emergência antes de iniciar o atendimento.");
		}
	}

	public PrioridadeEmergencia getPrioridade() {
		return prioridade;
	}

	public LocalDateTime getDataHoraChegada() {
		return dataHoraChegada;
	}

	public SituacaoEmergencia getSituacao() {
		return situacao;
	}

}
