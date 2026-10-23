package br.pucminas.hospital.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "consulta_rotina")
public class ConsultaRotina extends Consulta {

	protected ConsultaRotina() {
	}

	public ConsultaRotina(Paciente paciente, ProfissionalSaude profissionalResponsavel, LocalDate data,
			LocalTime horario, String motivo) {
		super(paciente, profissionalResponsavel, data, horario, motivo);
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
