package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * Momento que cada modalidade usa para se situar no histórico do paciente.
 */
class DataHoraReferenciaTest {

	private final Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
	private final ProfissionalSaude clinico = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
	private final Cirurgiao cirurgiao = new Cirurgiao("Dra. Carla Mendes", "CRM-MG 2", AreaAtuacao.CIRURGIA_GERAL);

	@Test
	void consultaUsaDataEHorario() {
		Consulta consulta = new ConsultaRotina(paciente, clinico, LocalDate.of(2030, 1, 15), LocalTime.of(14, 30), "Rotina");

		assertThat(consulta.getDataHoraReferencia()).isEqualTo(LocalDateTime.of(2030, 1, 15, 14, 30));
	}

	@Test
	void internacaoUsaOInicioDoDiaDeEntrada() {
		Quarto quarto = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 2);
		Internacao internacao = new Internacao(paciente, clinico, quarto, LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 3));

		assertThat(internacao.getDataHoraReferencia()).isEqualTo(LocalDateTime.of(2030, 2, 1, 0, 0));
	}

	@Test
	void emergenciaUsaAChegada() {
		LocalDateTime chegada = LocalDateTime.of(2030, 1, 15, 22, 40);
		AtendimentoEmergencia emergencia = new AtendimentoEmergencia(paciente, clinico, null, chegada);

		assertThat(emergencia.getDataHoraReferencia()).isEqualTo(chegada);
	}

	@Test
	void procedimentoUsaAPrevisaoAteSerIniciadoEDepoisOInicioReal() {
		LocalDateTime prevista = LocalDateTime.of(2030, 3, 10, 8, 0);
		ProcedimentoCirurgico procedimento = new ProcedimentoCirurgico(paciente, cirurgiao, "Apendicectomia",
				TipoCirurgia.ELETIVA, prevista);
		assertThat(procedimento.getDataHoraReferencia()).isEqualTo(prevista);

		procedimento.iniciar(prevista.plusMinutes(40));

		assertThat(procedimento.getDataHoraReferencia()).isEqualTo(prevista.plusMinutes(40));
	}

	@Test
	void situacoesAtivasDaConsulta() {
		assertThat(SituacaoConsulta.ativas()).containsExactlyInAnyOrder(SituacaoConsulta.AGENDADA,
				SituacaoConsulta.CONFIRMADA, SituacaoConsulta.EM_ATENDIMENTO);
	}

}
