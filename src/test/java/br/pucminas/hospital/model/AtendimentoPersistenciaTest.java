package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
		Quarto quarto = em.persist(new Quarto("201", 2, 2));

		Atendimento carregado = salvarERecarregar(new Internacao(paciente, cardiologista, quarto, DATA, DATA.plusDays(3)));

		assertThat(carregado).isInstanceOfSatisfying(Internacao.class, i -> {
			assertThat(i.getQuarto().getNumero()).isEqualTo("201");
			assertThat(i.getQuarto().getOcupacaoAtual()).isEqualTo(1);
		});
	}

}
