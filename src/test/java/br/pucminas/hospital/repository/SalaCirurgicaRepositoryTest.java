package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.SalaCirurgica;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SalaCirurgicaRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private SalaCirurgicaRepository repository;

	@Test
	void listaSoAsAtivasEmOrdemDeIdentificacao() {
		em.persist(new SalaCirurgica("Sala 2"));
		SalaCirurgica inativa = new SalaCirurgica("Sala 3");
		inativa.inativar();
		em.persist(inativa);
		em.persist(new SalaCirurgica("Sala 1"));
		em.flush();
		em.clear();

		assertThat(repository.findByAtivaTrueOrderByIdentificacaoAsc())
				.extracting(SalaCirurgica::getIdentificacao)
				.containsExactly("Sala 1", "Sala 2");
	}

	@Test
	void buscaPelaIdentificacao() {
		em.persist(new SalaCirurgica("Sala 1"));
		em.flush();
		em.clear();

		assertThat(repository.findByIdentificacao("Sala 1")).isPresent();
		assertThat(repository.findByIdentificacao("Sala 9")).isEmpty();
	}

}
