package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
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
class QuartoRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private QuartoRepository repository;

	@BeforeEach
	void cadastrar() {
		Quarto utiLotada = new Quarto("201", 2, TipoQuarto.UTI, 1);
		utiLotada.ocupar();
		em.persist(utiLotada);
		Quarto apartamentoComVaga = new Quarto("302", 3, TipoQuarto.APARTAMENTO, 2);
		apartamentoComVaga.ocupar();
		em.persist(apartamentoComVaga);
		em.persist(new Quarto("101", 1, TipoQuarto.ENFERMARIA, 6));
		em.persist(new Quarto("301", 3, TipoQuarto.APARTAMENTO, 2));
		em.flush();
		em.clear();
	}

	@Test
	void buscaPorNumero() {
		assertThat(repository.findByNumero("302")).get().extracting(Quarto::getOcupacaoAtual).isEqualTo(1);
		assertThat(repository.findByNumero("999")).isEmpty();
	}

	@Test
	void quartosComVagaEmOrdemDeNumero() {
		assertThat(repository.findComVaga()).extracting(Quarto::getNumero).containsExactly("101", "301", "302");
	}

	@Test
	void quartosPorTipo() {
		assertThat(repository.findByTipoOrderByNumeroAsc(TipoQuarto.APARTAMENTO)).extracting(Quarto::getNumero)
				.containsExactly("301", "302");
		assertThat(repository.findByTipoOrderByNumeroAsc(TipoQuarto.UTI)).extracting(Quarto::getNumero)
				.containsExactly("201");
	}

}
