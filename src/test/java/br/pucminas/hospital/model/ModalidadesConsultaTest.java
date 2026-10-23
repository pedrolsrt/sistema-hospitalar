package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.RegraNegocioException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ModalidadesConsultaTest {

	private static final LocalDate DATA = LocalDate.of(2030, 1, 15);
	private static final LocalTime HORARIO = LocalTime.of(14, 0);

	private final Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));

	static Stream<ProfissionalSaude> profissionaisDeCadaCategoria() {
		return Stream.of(
				new ClinicoGeral("Dra. Ana Lima", "CRM-MG 11111"),
				new MedicoEspecialista("Dr. João Souza", "CRM-MG 22222", AreaAtuacao.CARDIOLOGIA),
				new Cirurgiao("Dra. Carla Mendes", "CRM-MG 33333", AreaAtuacao.CIRURGIA_GERAL));
	}

	@Nested
	class ConsultaRotinaTest {

		@ParameterizedTest
		@MethodSource("br.pucminas.hospital.model.ModalidadesConsultaTest#profissionaisDeCadaCategoria")
		void qualquerProfissionalAtivoRealiza(ProfissionalSaude profissional) {
			Consulta consulta = new ConsultaRotina(paciente, profissional, DATA, HORARIO, "Check-up anual");

			consulta.validarRealizacao();
		}

	}

	@Nested
	class ConsultaEspecializadaTest {

		private ConsultaEspecializada consultaDeCardiologiaCom(ProfissionalSaude profissional) {
			return new ConsultaEspecializada(paciente, profissional, DATA, HORARIO, "Dor no peito",
					AreaAtuacao.CARDIOLOGIA);
		}

		@Test
		void especialistaDaMesmaAreaRealiza() {
			ConsultaEspecializada consulta = consultaDeCardiologiaCom(
					new MedicoEspecialista("Dr. João Souza", "CRM-MG 22222", AreaAtuacao.CARDIOLOGIA));
			consulta.confirmar();

			consulta.iniciarAtendimento();

			assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.EM_ATENDIMENTO);
		}

		@Test
		void especialistaDeOutraAreaNaoRealiza() {
			ConsultaEspecializada consulta = consultaDeCardiologiaCom(
					new MedicoEspecialista("Dr. Pedro Alves", "CRM-MG 44444", AreaAtuacao.NEUROLOGIA));

			assertThatThrownBy(consulta::validarRealizacao)
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O profissional Dr. Pedro Alves não pode realizar consulta especializada em "
							+ "Cardiologia: é necessário um médico especialista dessa área.");
		}

		@Test
		void cirurgiaoDaMesmaAreaNaoRealiza() {
			ConsultaEspecializada consulta = consultaDeCardiologiaCom(
					new Cirurgiao("Dra. Carla Mendes", "CRM-MG 33333", AreaAtuacao.CARDIOLOGIA));

			assertThatThrownBy(consulta::validarRealizacao).isInstanceOf(RegraNegocioException.class);
		}

		@Test
		void clinicoGeralNaoRealiza() {
			ConsultaEspecializada consulta = consultaDeCardiologiaCom(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 11111"));

			assertThatThrownBy(consulta::validarRealizacao).isInstanceOf(RegraNegocioException.class);
		}

		@Test
		void naoIniciaAtendimentoComProfissionalInadequado() {
			ConsultaEspecializada consulta = consultaDeCardiologiaCom(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 11111"));
			consulta.confirmar();

			assertThatThrownBy(consulta::iniciarAtendimento).isInstanceOf(RegraNegocioException.class);
			assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.CONFIRMADA);
		}

		@Test
		void exigeAreaDeAtuacao() {
			ProfissionalSaude especialista =
					new MedicoEspecialista("Dr. João Souza", "CRM-MG 22222", AreaAtuacao.CARDIOLOGIA);

			assertThatThrownBy(() -> new ConsultaEspecializada(paciente, especialista, DATA, HORARIO, "Dor no peito", null))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("A área de atuação da consulta especializada é obrigatória.");
		}

	}

}
