package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;

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
class ProfissionalSaudePersistenciaTest {

	@Autowired
	private TestEntityManager em;

	private ProfissionalSaude salvarERecarregar(ProfissionalSaude profissional) {
		Long id = em.persistAndGetId(profissional, Long.class);
		em.flush();
		em.clear();
		return em.find(ProfissionalSaude.class, id);
	}

	@Test
	void recarregaClinicoGeralComOTipoCorreto() {
		ProfissionalSaude carregado = salvarERecarregar(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 11111"));

		assertThat(carregado).isInstanceOf(ClinicoGeral.class);
		assertThat(carregado.getRegistroProfissional()).isEqualTo("CRM-MG 11111");
	}

	@Test
	void recarregaMedicoEspecialistaComSuaArea() {
		ProfissionalSaude carregado = salvarERecarregar(
				new MedicoEspecialista("Dr. João Souza", "CRM-MG 22222", AreaAtuacao.NEUROLOGIA));

		assertThat(carregado).isInstanceOfSatisfying(MedicoEspecialista.class,
				medico -> assertThat(medico.getAreaAtuacao()).isEqualTo(AreaAtuacao.NEUROLOGIA));
	}

	@Test
	void recarregaCirurgiaoComSuaAreaCirurgica() {
		ProfissionalSaude carregado = salvarERecarregar(
				new Cirurgiao("Dra. Carla Mendes", "CRM-MG 33333", AreaAtuacao.NEUROCIRURGIA));

		assertThat(carregado).isInstanceOfSatisfying(Cirurgiao.class,
				cirurgiao -> assertThat(cirurgiao.getAreaCirurgica()).isEqualTo(AreaAtuacao.NEUROCIRURGIA));
		assertThat(carregado.podeRealizarProcedimentos()).isTrue();
	}

}
