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
@Table(name = "consulta_rotina")
public class ConsultaRotina extends Consulta {

	@Enumerated(EnumType.STRING)
	@Column(name = "area_encaminhamento", length = 30)
	private AreaAtuacao areaEncaminhamento;

	protected ConsultaRotina() {
	}

	public ConsultaRotina(Paciente paciente, ProfissionalSaude profissionalResponsavel, LocalDate data,
			LocalTime horario, String motivo) {
		super(paciente, profissionalResponsavel, data, horario, motivo);
	}

	/**
	 * Registra o encaminhamento do paciente para a área informada. Só o profissional que pode encaminhar
	 * (clínico geral) o faz, e apenas durante o atendimento ou depois da conclusão da consulta. Um novo
	 * encaminhamento substitui o anterior.
	 */
	@Override
	public void encaminhar(AreaAtuacao area) {
		if (area == null) {
			throw new RegraNegocioException("Informe a área do atendimento especializado.");
		}
		ProfissionalSaude profissional = getProfissionalResponsavel();
		if (!profissional.podeEncaminharParaEspecialista()) {
			throw new RegraNegocioException("O profissional " + profissional.getNome()
					+ " não pode encaminhar pacientes para atendimento especializado: é necessário um clínico geral.");
		}
		if (!getSituacao().permiteRegistrarObservacoes()) {
			throw new RegraNegocioException("O encaminhamento só pode ser registrado durante o atendimento "
					+ "ou depois da conclusão da consulta. Situação atual: " + getSituacao().getDescricao() + ".");
		}
		this.areaEncaminhamento = area;
	}

	@Override
	public AreaAtuacao getAreaEncaminhamento() {
		return areaEncaminhamento;
	}

	@Override
	public ModalidadeAtendimento getModalidade() {
		return ModalidadeAtendimento.CONSULTA_ROTINA;
	}

	// Qualquer profissional ativo realiza consulta de rotina; as verificações comuns já bastam.
	@Override
	protected void validarCondicoesEspecificas() {
	}

}
