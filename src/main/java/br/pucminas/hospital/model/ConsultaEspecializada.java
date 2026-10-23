package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "consulta_especializada")
public class ConsultaEspecializada extends Consulta {

	@Enumerated(EnumType.STRING)
	@Column(name = "area_atuacao", nullable = false, length = 30)
	private AreaAtuacao areaAtuacao;

	protected ConsultaEspecializada() {
	}

	/**
	 * @throws RegraNegocioException se a área de atuação não for informada
	 */
	public ConsultaEspecializada(Paciente paciente, ProfissionalSaude profissionalResponsavel, LocalDate data,
			LocalTime horario, String motivo, AreaAtuacao areaAtuacao) {
		super(paciente, profissionalResponsavel, data, horario, motivo);
		if (areaAtuacao == null) {
			throw new RegraNegocioException("A área de atuação da consulta especializada é obrigatória.");
		}
		this.areaAtuacao = areaAtuacao;
	}

	/**
	 * Exige um médico especialista na área da consulta.
	 */
	@Override
	protected void validarCondicoesEspecificas() {
		ProfissionalSaude profissional = getProfissionalResponsavel();
		if (!profissional.podeRealizarConsultaEspecializada(areaAtuacao)) {
			throw new RegraNegocioException("O profissional " + profissional.getNome()
					+ " não pode realizar consulta especializada em " + areaAtuacao.getDescricao()
					+ ": é necessário um médico especialista dessa área.");
		}
	}

	@Override
	public ModalidadeAtendimento getModalidade() {
		return ModalidadeAtendimento.CONSULTA_ESPECIALIZADA;
	}

	@Override
	public AreaAtuacao getAreaAtuacao() {
		return areaAtuacao;
	}

}
