package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * {@link Atendimento#getSituacao()} devolve a situação de cada modalidade pela mesma interface.
 */
class SituacaoPolimorficaTest {

	private static final LocalDate DIA = LocalDate.of(2030, 3, 10);

	private final Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
	private final ProfissionalSaude clinico = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");

	@Test
	void consultaInformaASuaSituacao() {
		Atendimento consulta = new ConsultaRotina(paciente, clinico, DIA, LocalTime.of(9, 0), "Retorno");

		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.AGENDADA);
	}

	@Test
	void emergenciaInformaASuaSituacao() {
		AtendimentoEmergencia emergencia = new AtendimentoEmergencia(paciente, clinico, PrioridadeEmergencia.ALTA,
				LocalDateTime.of(2030, 3, 10, 22, 0));
		emergencia.iniciar();

		assertThat(((Atendimento) emergencia).getSituacao()).isEqualTo(SituacaoEmergencia.EM_ATENDIMENTO);
	}

	@Test
	void procedimentoInformaASuaSituacao() {
		Cirurgiao cirurgiao = new Cirurgiao("Dra. Carla Mendes", "CRM-MG 2", AreaAtuacao.CIRURGIA_GERAL);
		ProcedimentoCirurgico procedimento = new ProcedimentoCirurgico(paciente, cirurgiao, "Apendicectomia",
				TipoCirurgia.ELETIVA, new SalaCirurgica("Sala 1"), DIA.atTime(8, 0), 60);
		procedimento.cancelar();

		assertThat(((Atendimento) procedimento).getSituacao()).isEqualTo(SituacaoProcedimento.CANCELADO);
	}

	@Test
	void internacaoEAtivaAteAAltaEEncerradaDepois() {
		Internacao internacao = new Internacao(paciente, clinico, new Quarto("101", 1, TipoQuarto.ENFERMARIA, 2), DIA,
				DIA.plusDays(3));
		assertThat(((Atendimento) internacao).getSituacao()).isEqualTo(SituacaoInternacao.ATIVA);

		internacao.darAlta(DIA.plusDays(2));

		assertThat(internacao.getSituacao()).isEqualTo(SituacaoInternacao.ENCERRADA);
	}

	@Test
	void situacaoDaInternacaoSegueAMaquinaDeEstados() {
		assertThat(SituacaoInternacao.ATIVA.podeMudarPara(SituacaoInternacao.ENCERRADA)).isTrue();
		assertThat(SituacaoInternacao.ATIVA.isFinal()).isFalse();
		assertThat(SituacaoInternacao.ENCERRADA.podeMudarPara(SituacaoInternacao.ATIVA)).isFalse();
		assertThat(SituacaoInternacao.ENCERRADA.isFinal()).isTrue();
		assertThat(SituacaoInternacao.ATIVA.getDescricao()).isEqualTo("Ativa");
	}

	@Test
	void soAEmergenciaTemPrioridade() {
		Atendimento consulta = new ConsultaRotina(paciente, clinico, DIA, LocalTime.of(9, 0), "Retorno");
		Atendimento emergencia = new AtendimentoEmergencia(paciente, clinico, PrioridadeEmergencia.CRITICA,
				LocalDateTime.of(2030, 3, 10, 22, 0));

		assertThat(consulta.getPrioridade()).isNull();
		assertThat(emergencia.getPrioridade()).isEqualTo(PrioridadeEmergencia.CRITICA);
	}

}
