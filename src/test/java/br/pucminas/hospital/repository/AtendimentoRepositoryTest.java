package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.Atendimento;
import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.Cirurgiao;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.Internacao;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.PrioridadeEmergencia;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoCirurgia;
import br.pucminas.hospital.model.TipoQuarto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AtendimentoRepositoryTest {

	private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

	@Autowired
	private TestEntityManager em;

	@Autowired
	private AtendimentoRepository repository;

	@Test
	void historicoDoPacienteReuneTodasAsModalidadesEmOrdemCronologica() {
		Paciente maria = em.persist(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)));
		Paciente jose = em.persist(new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1)));
		ProfissionalSaude clinico = em.persist(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1"));
		Cirurgiao cirurgiao = em.persist(new Cirurgiao("Dra. Carla Mendes", "CRM-MG 2", AreaAtuacao.CIRURGIA_GERAL));
		Quarto quarto = em.persist(new Quarto("101", 1, TipoQuarto.ENFERMARIA, 4));

		// Cadastrados fora da ordem cronológica, para que a ordem por id não coincida com a esperada.
		em.persist(new ConsultaRotina(maria, clinico, DIA.plusDays(2), LocalTime.of(10, 0), "Retorno"));
		ProcedimentoCirurgico cirurgia = new ProcedimentoCirurgico(maria, cirurgiao, "Apendicectomia",
				TipoCirurgia.URGENCIA, DIA.plusDays(1).atTime(8, 0));
		cirurgia.iniciar(DIA.plusDays(1).atTime(7, 30));
		em.persist(cirurgia);
		em.persist(new Internacao(maria, clinico, quarto, DIA, DIA.plusDays(4)));
		em.persist(new AtendimentoEmergencia(maria, clinico, PrioridadeEmergencia.ALTA, DIA.minusDays(1).atTime(22, 0)));
		em.persist(new ConsultaRotina(jose, clinico, DIA, LocalTime.of(9, 0), "Outro paciente"));
		em.flush();
		em.clear();

		List<Atendimento> historico = repository.findHistoricoDoPaciente(maria.getId());

		assertThat(historico).hasExactlyElementsOfTypes(AtendimentoEmergencia.class, Internacao.class,
				ProcedimentoCirurgico.class, ConsultaRotina.class);
		assertThat(historico).extracting(Atendimento::getDataHoraReferencia).isSorted();
	}

	@Test
	void historicoVazioParaPacienteSemAtendimentos() {
		Paciente maria = em.persist(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)));

		assertThat(repository.findHistoricoDoPaciente(maria.getId())).isEmpty();
	}

}
