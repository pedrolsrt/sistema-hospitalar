package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.RegraNegocioException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Template Method de {@link Atendimento#validarRealizacao()}, com uma modalidade de teste que registra
 * se as condições específicas foram verificadas.
 */
class AtendimentoTest {

	private static class AtendimentoDeTeste extends Atendimento {

		private int verificacoesEspecificas;
		private RuntimeException falhaEspecifica;

		AtendimentoDeTeste(Paciente paciente, ProfissionalSaude profissional) {
			super(paciente, profissional);
		}

		@Override
		public LocalDateTime getDataHoraReferencia() {
			return LocalDateTime.of(2030, 1, 1, 0, 0);
		}

		@Override
		protected void validarCondicoesEspecificas() {
			verificacoesEspecificas++;
			if (falhaEspecifica != null) {
				throw falhaEspecifica;
			}
		}

	}

	private final Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
	private final ProfissionalSaude medico = new ClinicoGeral("Dr. João Souza", "CRM-MG 12345");
	private final AtendimentoDeTeste atendimento = new AtendimentoDeTeste(paciente, medico);

	@Test
	void verificaCondicoesEspecificasQuandoAsComunsSaoAtendidas() {
		atendimento.validarRealizacao();

		assertThat(atendimento.verificacoesEspecificas).isEqualTo(1);
	}

	@Test
	void pacienteInativoInterrompeAntesDasCondicoesEspecificas() {
		paciente.inativar();

		assertThatThrownBy(atendimento::validarRealizacao)
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O paciente Maria Silva está inativo e não pode ser atendido.");
		assertThat(atendimento.verificacoesEspecificas).isZero();
	}

	@Test
	void profissionalInativoInterrompeAntesDasCondicoesEspecificas() {
		medico.inativar();

		assertThatThrownBy(atendimento::validarRealizacao)
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O profissional Dr. João Souza está inativo e não pode realizar atendimentos.");
		assertThat(atendimento.verificacoesEspecificas).isZero();
	}

	@Test
	void falhaNasCondicoesEspecificasEPropagada() {
		atendimento.falhaEspecifica = new RegraNegocioException("Condição específica não atendida.");

		assertThatThrownBy(atendimento::validarRealizacao).isSameAs(atendimento.falhaEspecifica);
	}

	@Test
	void registraObservacoesSemRestricaoPorPadrao() {
		atendimento.registrarObservacoes("Paciente estável.");

		assertThat(atendimento.getObservacoes()).isEqualTo("Paciente estável.");
	}

}
