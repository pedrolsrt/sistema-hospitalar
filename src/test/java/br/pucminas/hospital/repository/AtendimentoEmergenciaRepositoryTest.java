package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.PrioridadeEmergencia;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.SituacaoEmergencia;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class AtendimentoEmergenciaRepositoryTest {

	private static final LocalDateTime NOITE = LocalDateTime.of(2030, 1, 15, 22, 0);

	@Autowired
	private TestEntityManager em;

	@Autowired
	private AtendimentoEmergenciaRepository repository;

	private Paciente paciente;
	private ProfissionalSaude plantonista;

	@BeforeEach
	void cadastrar() {
		paciente = em.persist(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)));
		plantonista = em.persist(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1"));

		// Aguardando avaliação, cadastrados fora de ordem de chegada.
		aguardando(PrioridadeEmergencia.BAIXA, 30);
		aguardando(PrioridadeEmergencia.CRITICA, 50);
		aguardando(null, 0);
		aguardando(PrioridadeEmergencia.ALTA, 40);
		aguardando(PrioridadeEmergencia.CRITICA, 45);
		aguardando(PrioridadeEmergencia.MODERADA, 10);
		aguardando(PrioridadeEmergencia.ALTA, 20);
		aguardando(null, 5);

		// Fora da fila: já em atendimento ou concluídos, mesmo que críticos e mais antigos.
		AtendimentoEmergencia emAtendimento = novo(PrioridadeEmergencia.CRITICA, -60);
		emAtendimento.iniciar();
		em.persist(emAtendimento);
		AtendimentoEmergencia concluido = novo(PrioridadeEmergencia.CRITICA, -90);
		concluido.iniciar();
		concluido.concluir();
		em.persist(concluido);

		em.flush();
		em.clear();
	}

	private AtendimentoEmergencia novo(PrioridadeEmergencia prioridade, int minutosDepoisDasDez) {
		return new AtendimentoEmergencia(paciente, plantonista, prioridade, NOITE.plusMinutes(minutosDepoisDasDez));
	}

	private void aguardando(PrioridadeEmergencia prioridade, int minutosDepoisDasDez) {
		em.persist(novo(prioridade, minutosDepoisDasDez));
	}

	@Test
	void aguardandoAvaliacaoSaoSoOsQueAindaNaoForamIniciados() {
		assertThat(repository.findBySituacaoOrderByDataHoraChegadaAsc(SituacaoEmergencia.AGUARDANDO_AVALIACAO))
				.hasSize(8)
				.extracting(AtendimentoEmergencia::getSituacao)
				.containsOnly(SituacaoEmergencia.AGUARDANDO_AVALIACAO);
	}

	@Test
	void listaPorSituacaoEmOrdemDeChegada() {
		assertThat(repository.findBySituacaoOrderByDataHoraChegadaAsc(SituacaoEmergencia.EM_ATENDIMENTO))
				.extracting(AtendimentoEmergencia::getDataHoraChegada)
				.containsExactly(NOITE.minusMinutes(60));
		assertThat(repository.findBySituacaoOrderByDataHoraChegadaAsc(SituacaoEmergencia.AGUARDANDO_AVALIACAO))
				.extracting(AtendimentoEmergencia::getDataHoraChegada)
				.isSorted()
				.hasSize(8);
	}

	@Test
	void verificaEmergenciaAtivaDePacienteEDeProfissional() {
		assertThat(repository.existeAtivoDoPaciente(paciente.getId())).isTrue();
		assertThat(repository.existeAtivoDoProfissional(plantonista.getId())).isTrue();

		Paciente semEmergencia = em.persist(new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1)));
		ProfissionalSaude outro = em.persist(new ClinicoGeral("Dr. Paulo Reis", "CRM-MG 2"));
		AtendimentoEmergencia concluido = new AtendimentoEmergencia(semEmergencia, outro, PrioridadeEmergencia.BAIXA,
				NOITE);
		concluido.iniciar();
		concluido.concluir();
		em.persist(concluido);
		em.flush();

		assertThat(repository.existeAtivoDoPaciente(semEmergencia.getId())).isFalse();
		assertThat(repository.existeAtivoDoProfissional(outro.getId())).isFalse();
	}
}
