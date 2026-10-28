package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.Paciente;
import java.time.LocalDate;
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
class PacienteRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private PacienteRepository repository;

	@BeforeEach
	void cadastrar() {
		em.persist(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)));
		em.persist(new Paciente("Ana Costa", "11144477735", LocalDate.of(1985, 2, 1)));
		Paciente inativo = new Paciente("Bruno Rocha", "39053344705", LocalDate.of(1970, 9, 30));
		inativo.inativar();
		em.persist(inativo);
		em.flush();
		em.clear();
	}

	@Test
	void verificaCpfJaCadastrado() {
		assertThat(repository.existsByCpf("52998224725")).isTrue();
		assertThat(repository.existsByCpf("39053344705")).isTrue();
		assertThat(repository.existsByCpf("00000000000")).isFalse();
	}

	@Test
	void listaApenasAtivosPorNome() {
		assertThat(repository.findByAtivoTrueOrderByNomeAsc()).extracting(Paciente::getNome)
				.containsExactly("Ana Costa", "Maria Silva");
	}

}
