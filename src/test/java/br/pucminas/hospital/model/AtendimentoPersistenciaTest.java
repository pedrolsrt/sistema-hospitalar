package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;

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

// Usa o H2 do perfil test (modo PostgreSQL, com as migrations) em vez do banco embarcado padrão do @DataJpaTest.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AtendimentoPersistenciaTest {

	private static final LocalDate DATA = LocalDate.of(2030, 1, 15);

	@Autowired
	private TestEntityManager em;

	private Paciente paciente;
	private MedicoEspecialista cardiologista;

	@BeforeEach
	void cadastrar() {
		paciente = em.persist(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)));
		cardiologista = em.persist(new MedicoEspecialista("Dr. João Souza", "CRM-MG 22222", AreaAtuacao.CARDIOLOGIA));
	}

	private Atendimento salvarERecarregar(Atendimento atendimento) {
		Long id = em.persistAndGetId(atendimento, Long.class);
		em.flush();
		em.clear();
		return em.find(Atendimento.class, id);
	}

	@Test
	void recarregaConsultaDeRotinaComSituacaoEObservacoes() {
		ConsultaRotina consulta = new ConsultaRotina(paciente, cardiologista, DATA, LocalTime.of(9, 0), "Check-up");
		consulta.confirmar();
		consulta.iniciarAtendimento();
		consulta.registrarObservacoes("Sem alterações.");

		Atendimento carregado = salvarERecarregar(consulta);

		assertThat(carregado).isInstanceOfSatisfying(ConsultaRotina.class, c -> {
			assertThat(c.getSituacao()).isEqualTo(SituacaoConsulta.EM_ATENDIMENTO);
			assertThat(c.getObservacoes()).isEqualTo("Sem alterações.");
			assertThat(c.getHorario()).isEqualTo(LocalTime.of(9, 0));
		});
	}

	@Test
	void recarregaConsultaEspecializadaComSuaArea() {
		Atendimento carregado = salvarERecarregar(new ConsultaEspecializada(paciente, cardiologista, DATA,
				LocalTime.of(10, 0), "Dor no peito", AreaAtuacao.CARDIOLOGIA));

		assertThat(carregado).isInstanceOfSatisfying(ConsultaEspecializada.class,
				c -> assertThat(c.getAreaAtuacao()).isEqualTo(AreaAtuacao.CARDIOLOGIA));
		assertThat(carregado.getProfissionalResponsavel().podeRealizarConsultaEspecializada(AreaAtuacao.CARDIOLOGIA))
				.isTrue();
	}

	@Test
	void recarregaInternacaoComQuarto() {
		Quarto quarto = em.persist(new Quarto("201", 2, TipoQuarto.ENFERMARIA, 2));

		Atendimento carregado = salvarERecarregar(new Internacao(paciente, cardiologista, quarto, DATA, DATA.plusDays(3)));

		assertThat(carregado).isInstanceOfSatisfying(Internacao.class, i -> {
			assertThat(i.getQuarto().getNumero()).isEqualTo("201");
			assertThat(i.getQuarto().getOcupacaoAtual()).isEqualTo(1);
		});
	}

	@Test
	void recarregaAtendimentoDeEmergenciaSemPrioridadeDefinida() {
		LocalDateTime chegada = LocalDateTime.of(2030, 1, 15, 22, 40);

		Atendimento carregado = salvarERecarregar(new AtendimentoEmergencia(paciente, cardiologista, null, chegada));

		assertThat(carregado).isInstanceOfSatisfying(AtendimentoEmergencia.class, e -> {
			assertThat(e.getPrioridade()).isNull();
			assertThat(e.getSituacao()).isEqualTo(SituacaoEmergencia.AGUARDANDO_AVALIACAO);
			assertThat(e.getDataHoraChegada()).isEqualTo(chegada);
		});
	}

	@Test
	void recarregaAtendimentoDeEmergenciaEmAtendimento() {
		AtendimentoEmergencia emergencia = new AtendimentoEmergencia(paciente, cardiologista,
				PrioridadeEmergencia.CRITICA, LocalDateTime.of(2030, 1, 15, 22, 40));
		emergencia.iniciar();

		Atendimento carregado = salvarERecarregar(emergencia);

		assertThat(carregado).isInstanceOfSatisfying(AtendimentoEmergencia.class, e -> {
			assertThat(e.getPrioridade()).isEqualTo(PrioridadeEmergencia.CRITICA);
			assertThat(e.getSituacao()).isEqualTo(SituacaoEmergencia.EM_ATENDIMENTO);
		});
	}

	@Test
	void recarregaProcedimentoCirurgicoConcluido() {
		Cirurgiao cirurgiao = em.persist(new Cirurgiao("Dra. Carla Mendes", "CRM-MG 33333", AreaAtuacao.CIRURGIA_GERAL));
		LocalDateTime inicio = LocalDateTime.of(2030, 3, 10, 8, 15);
		ProcedimentoCirurgico procedimento = new ProcedimentoCirurgico(paciente, cirurgiao, "Apendicectomia",
				TipoCirurgia.URGENCIA, inicio.minusMinutes(15));
		procedimento.registrarObservacoesPreOperatorias("Jejum de 8 horas.");
		procedimento.iniciar(inicio);
		procedimento.concluir(inicio.plusHours(1));
		procedimento.registrarObservacoesPosOperatorias("Sem intercorrências.");

		Atendimento carregado = salvarERecarregar(procedimento);

		assertThat(carregado).isInstanceOfSatisfying(ProcedimentoCirurgico.class, p -> {
			assertThat(p.getSituacao()).isEqualTo(SituacaoProcedimento.CONCLUIDO);
			assertThat(p.getTipoCirurgia()).isEqualTo(TipoCirurgia.URGENCIA);
			assertThat(p.getDataHoraInicio()).isEqualTo(inicio);
			assertThat(p.getDataHoraFim()).isEqualTo(inicio.plusHours(1));
			assertThat(p.getObservacoesPreOperatorias()).isEqualTo("Jejum de 8 horas.");
			assertThat(p.getObservacoesPosOperatorias()).isEqualTo("Sem intercorrências.");
		});
		assertThat(carregado.getProfissionalResponsavel().podeRealizarProcedimentos()).isTrue();
	}

}
