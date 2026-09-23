package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.TransicaoStatusInvalidaException;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ConsultaTest {

	private Consulta novaConsulta() {
		Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
		ProfissionalSaude medico = new ProfissionalSaude("Dr. João Souza", "CRM-MG 12345", "Cardiologia");
		return new Consulta(paciente, medico, LocalDate.of(2030, 1, 15), LocalTime.of(14, 0), "Dor no peito");
	}

	@Test
	void novaConsultaComecaAgendada() {
		assertThat(novaConsulta().getStatus()).isEqualTo(StatusConsulta.AGENDADA);
	}

	@Test
	void realizarConsultaAgendada() {
		Consulta consulta = novaConsulta();

		consulta.realizar();

		assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
	}

	@Test
	void cancelarConsultaAgendada() {
		Consulta consulta = novaConsulta();

		consulta.cancelar();

		assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.CANCELADA);
	}

	@Test
	void naoRealizaConsultaCancelada() {
		Consulta consulta = novaConsulta();
		consulta.cancelar();

		assertThatThrownBy(consulta::realizar)
				.isInstanceOf(TransicaoStatusInvalidaException.class)
				.hasMessageStartingWith("Não é possível realizar uma consulta cancelada.");
		assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.CANCELADA);
	}

	@Test
	void naoRealizaConsultaJaRealizada() {
		Consulta consulta = novaConsulta();
		consulta.realizar();

		assertThatThrownBy(consulta::realizar).isInstanceOf(TransicaoStatusInvalidaException.class);
	}

	@Test
	void naoCancelaConsultaRealizada() {
		Consulta consulta = novaConsulta();
		consulta.realizar();

		assertThatThrownBy(consulta::cancelar)
				.isInstanceOf(TransicaoStatusInvalidaException.class)
				.hasMessageStartingWith("Não é possível cancelar uma consulta realizada.");
		assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
	}

	@Test
	void naoCancelaConsultaJaCancelada() {
		Consulta consulta = novaConsulta();
		consulta.cancelar();

		assertThatThrownBy(consulta::cancelar).isInstanceOf(TransicaoStatusInvalidaException.class);
	}

}
