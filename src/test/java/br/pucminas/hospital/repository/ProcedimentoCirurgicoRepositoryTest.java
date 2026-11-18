package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.Cirurgiao;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.Periodo;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.model.SalaCirurgica;
import br.pucminas.hospital.model.TipoCirurgia;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class ProcedimentoCirurgicoRepositoryTest {

	private static final LocalDate DIA = LocalDate.of(2030, 3, 10);

	@Autowired
	private TestEntityManager em;

	@Autowired
	private ProcedimentoCirurgicoRepository repository;

	private Paciente paciente;
	private Cirurgiao draCarla;
	private Cirurgiao drPaulo;
	private SalaCirurgica sala;

	@BeforeEach
	void cadastrar() {
		paciente = em.persist(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)));
		draCarla = em.persist(new Cirurgiao("Dra. Carla Mendes", "CRM-MG 1", AreaAtuacao.CIRURGIA_GERAL));
		drPaulo = em.persist(new Cirurgiao("Dr. Paulo Reis", "CRM-MG 2", AreaAtuacao.ORTOPEDIA));
		sala = em.persist(new SalaCirurgica("Sala 1"));
	}

	private ProcedimentoCirurgico agendar(Cirurgiao cirurgiao, String descricao, LocalDateTime prevista) {
		return new ProcedimentoCirurgico(paciente, cirurgiao, descricao, TipoCirurgia.ELETIVA, sala, prevista, 60);
	}

	private void concluido(Cirurgiao cirurgiao, String descricao, LocalDateTime inicio) {
		ProcedimentoCirurgico procedimento = agendar(cirurgiao, descricao, inicio);
		procedimento.iniciar(inicio);
		procedimento.concluir(inicio.plusHours(2));
		em.persist(procedimento);
	}

	private void salvarERecarregar() {
		em.flush();
		em.clear();
	}

	@Test
	void concluidosDoCirurgiaoDoMaisRecenteParaOMaisAntigo() {
		concluido(draCarla, "Colecistectomia", DIA.atTime(8, 0));
		concluido(draCarla, "Apendicectomia", DIA.plusDays(5).atTime(8, 0));
		concluido(drPaulo, "Artroscopia", DIA.atTime(10, 0));
		ProcedimentoCirurgico emAndamento = agendar(draCarla, "Herniorrafia", DIA.plusDays(6).atTime(8, 0));
		emAndamento.iniciar(DIA.plusDays(6).atTime(8, 5));
		em.persist(emAndamento);
		ProcedimentoCirurgico cancelado = agendar(draCarla, "Biópsia", DIA.plusDays(7).atTime(8, 0));
		cancelado.cancelar();
		em.persist(cancelado);
		em.persist(agendar(draCarla, "Tireoidectomia", DIA.plusDays(8).atTime(8, 0)));
		salvarERecarregar();

		assertThat(repository.findConcluidosDoCirurgiao(draCarla.getId()))
				.extracting(ProcedimentoCirurgico::getDescricao)
				.containsExactly("Apendicectomia", "Colecistectomia");
	}

	@Test
	void procedimentosPrevistosNaDataIncluindoOsLimitesDoDia() {
		em.persist(agendar(draCarla, "Fim do dia", DIA.atTime(LocalTime.of(23, 59))));
		em.persist(agendar(draCarla, "Início do dia", DIA.atStartOfDay()));
		em.persist(agendar(drPaulo, "Meio do dia", DIA.atTime(12, 0)));
		em.persist(agendar(draCarla, "Dia anterior", DIA.minusDays(1).atTime(23, 59)));
		em.persist(agendar(draCarla, "Dia seguinte", DIA.plusDays(1).atStartOfDay()));
		ProcedimentoCirurgico cancelado = agendar(drPaulo, "Cancelado no dia", DIA.atTime(15, 0));
		cancelado.cancelar();
		em.persist(cancelado);
		salvarERecarregar();

		assertThat(repository.findPrevistosNaData(DIA))
				.extracting(ProcedimentoCirurgico::getDescricao)
				.containsExactly("Início do dia", "Meio do dia", "Cancelado no dia", "Fim do dia");
	}

	@Test
	void verificaProcedimentoAtivoDePacienteEDeCirurgiao() {
		concluido(drPaulo, "Artroscopia", DIA.atTime(10, 0));
		ProcedimentoCirurgico cancelado = agendar(drPaulo, "Biópsia", DIA.atTime(15, 0));
		cancelado.cancelar();
		em.persist(cancelado);
		salvarERecarregar();
		assertThat(repository.existeAtivoDoProfissional(drPaulo.getId())).isFalse();
		assertThat(repository.existeAtivoDoPaciente(paciente.getId())).isFalse();

		ProcedimentoCirurgico emAndamento = agendar(draCarla, "Herniorrafia", DIA.atTime(8, 0));
		emAndamento.iniciar(DIA.atTime(8, 5));
		em.persist(emAndamento);
		salvarERecarregar();
		assertThat(repository.existeAtivoDoProfissional(draCarla.getId())).isTrue();
		assertThat(repository.existeAtivoDoPaciente(paciente.getId())).isTrue();
	}

	@Test
	void verificaProcedimentoAtivoNaSala() {
		SalaCirurgica sala2 = em.persist(new SalaCirurgica("Sala 2"));
		concluido(draCarla, "Colecistectomia", DIA.atTime(8, 0));
		salvarERecarregar();
		assertThat(repository.existeAtivoNaSala(sala.getId())).isFalse();

		em.persist(agendar(draCarla, "Herniorrafia", DIA.plusDays(1).atTime(8, 0)));
		salvarERecarregar();
		assertThat(repository.existeAtivoNaSala(sala.getId())).isTrue();
		assertThat(repository.existeAtivoNaSala(sala2.getId())).isFalse();
	}

	// Procedimentos de 60 minutos; a busca traz os ativos que começam até uma duração máxima (12 horas)
	// antes do período, e a sobreposição exata é decidida depois, por Periodo.sobrepoe.
	@Test
	void procedimentosAtivosProximosDoPeriodo() {
		Periodo tarde = new Periodo(DIA.atTime(14, 0), DIA.atTime(16, 0));
		em.persist(agendar(draCarla, "Doze horas antes", DIA.atTime(2, 0)));
		em.persist(agendar(draCarla, "Manhã", DIA.atTime(9, 0)));
		em.persist(agendar(draCarla, "Tarde", DIA.atTime(15, 0)));
		em.persist(agendar(draCarla, "No fim do período", DIA.atTime(16, 0)));
		em.persist(agendar(draCarla, "Depois", DIA.atTime(17, 0)));
		em.persist(agendar(draCarla, "Muito antes", DIA.atTime(1, 59)));
		ProcedimentoCirurgico cancelado = agendar(draCarla, "Cancelado", DIA.atTime(14, 30));
		cancelado.cancelar();
		em.persist(cancelado);
		concluido(draCarla, "Concluído", DIA.atTime(15, 30));
		em.persist(agendar(drPaulo, "Outro cirurgião", DIA.atTime(15, 0)));
		salvarERecarregar();

		assertThat(repository.findAtivosDoProfissionalProximosDe(draCarla.getId(), tarde))
				.extracting(ProcedimentoCirurgico::getDescricao)
				.containsExactlyInAnyOrder("Doze horas antes", "Manhã", "Tarde", "No fim do período");
		assertThat(repository.findAtivosDoPacienteProximosDe(paciente.getId(), tarde))
				.extracting(ProcedimentoCirurgico::getDescricao)
				.contains("Tarde", "Outro cirurgião")
				.doesNotContain("Cancelado", "Concluído", "Depois");
		assertThat(repository.findAtivosDaSalaProximosDe(sala.getId(), tarde)).hasSize(5);
	}
}
