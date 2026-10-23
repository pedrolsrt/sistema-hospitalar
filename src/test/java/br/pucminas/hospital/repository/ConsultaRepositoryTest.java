package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.SituacaoConsulta;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ConsultaRepositoryTest {

	private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

	@Autowired
	private TestEntityManager em;

	@Autowired
	private ConsultaRepository repository;

	private Paciente maria;
	private Paciente jose;
	private ProfissionalSaude drJoao;
	private ProfissionalSaude draAna;

	@BeforeEach
	void cadastrar() {
		maria = em.persist(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)));
		jose = em.persist(new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1)));
		drJoao = em.persist(new ClinicoGeral("Dr. João Souza", "CRM-MG 1"));
		draAna = em.persist(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 2"));

		// Agenda do Dr. João no dia: uma consulta em cada situação, cadastradas fora de ordem de horário.
		consulta(drJoao, jose, DIA, 14, SituacaoConsulta.NAO_COMPARECEU);
		consulta(drJoao, maria, DIA, 9, SituacaoConsulta.AGENDADA);
		consulta(drJoao, jose, DIA, 12, SituacaoConsulta.CANCELADA);
		consulta(drJoao, jose, DIA, 10, SituacaoConsulta.CONFIRMADA);
		consulta(drJoao, maria, DIA, 13, SituacaoConsulta.CONCLUIDA);
		consulta(drJoao, maria, DIA, 11, SituacaoConsulta.EM_ATENDIMENTO);
		// Outro dia e outro profissional.
		consulta(drJoao, maria, DIA.plusDays(1), 9, SituacaoConsulta.AGENDADA);
		consulta(draAna, maria, DIA, 8, SituacaoConsulta.AGENDADA);
		em.flush();
		em.clear();
	}

	private void consulta(ProfissionalSaude profissional, Paciente paciente, LocalDate data, int hora,
			SituacaoConsulta situacao) {
		Consulta consulta = new ConsultaRotina(paciente, profissional, data, LocalTime.of(hora, 0), "Rotina");
		switch (situacao) {
			case AGENDADA -> {
			}
			case CONFIRMADA -> consulta.confirmar();
			case EM_ATENDIMENTO -> {
				consulta.confirmar();
				consulta.iniciarAtendimento();
			}
			case CONCLUIDA -> {
				consulta.confirmar();
				consulta.iniciarAtendimento();
				consulta.concluir();
			}
			case CANCELADA -> consulta.cancelar();
			case NAO_COMPARECEU -> consulta.registrarNaoComparecimento();
		}
		em.persist(consulta);
	}

	@Test
	void consultasAtivasDoProfissionalNaData() {
		assertThat(repository.findAtivasDoProfissionalNaData(drJoao.getId(), DIA))
				.extracting(Consulta::getHorario)
				.containsExactlyInAnyOrder(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
	}

	@Test
	void consultasAtivasDoPacienteNaData() {
		assertThat(repository.findAtivasDoPacienteNaData(maria.getId(), DIA))
				.extracting(Consulta::getHorario)
				.containsExactlyInAnyOrder(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(11, 0));
	}

	@Test
	void consultasAtivasSoContamSituacoesQueOcupamHorario() {
		assertThat(repository.findAtivasDoProfissionalNaData(drJoao.getId(), DIA))
				.extracting(Consulta::getSituacao)
				.allMatch(SituacaoConsulta::isAtiva);
	}

	@Test
	void agendaDoProfissionalTemTodasAsSituacoesEmOrdemDeHorario() {
		assertThat(repository.findAgendaDoProfissional(drJoao.getId(), DIA))
				.extracting(Consulta::getHorario)
				.containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0),
						LocalTime.of(13, 0), LocalTime.of(14, 0));
	}

	@Test
	void agendaVaziaEmDiaSemConsultas() {
		assertThat(repository.findAgendaDoProfissional(draAna.getId(), DIA.plusDays(1))).isEmpty();
	}

}
