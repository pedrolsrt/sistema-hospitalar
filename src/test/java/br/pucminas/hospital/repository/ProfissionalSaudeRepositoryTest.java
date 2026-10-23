package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.Cirurgiao;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.MedicoEspecialista;
import br.pucminas.hospital.model.ProfissionalSaude;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProfissionalSaudeRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private ProfissionalSaudeRepository repository;

	@BeforeEach
	void cadastrar() {
		em.persist(new ClinicoGeral("Carlos Dias", "CRM-MG 1"));
		em.persist(new MedicoEspecialista("Bruno Alves", "CRM-MG 2", AreaAtuacao.CARDIOLOGIA));
		em.persist(new Cirurgiao("Ana Lima", "CRM-MG 3", AreaAtuacao.CARDIOLOGIA));
		em.persist(new MedicoEspecialista("Diana Melo", "CRM-MG 4", AreaAtuacao.NEUROLOGIA));
		em.persist(new Cirurgiao("Eduardo Reis", "CRM-MG 5", AreaAtuacao.ORTOPEDIA));
		MedicoEspecialista inativo = new MedicoEspecialista("Fábio Nunes", "CRM-MG 6", AreaAtuacao.CARDIOLOGIA);
		inativo.inativar();
		em.persist(inativo);
		em.flush();
		em.clear();
	}

	@Test
	void verificaRegistroJaCadastrado() {
		assertThat(repository.existsByRegistroProfissional("CRM-MG 6")).isTrue();
		assertThat(repository.existsByRegistroProfissional("CRM-MG 99")).isFalse();
	}

	@Test
	void listaApenasAtivosPorNome() {
		assertThat(repository.findByAtivoTrueOrderByNomeAsc()).extracting(ProfissionalSaude::getNome)
				.containsExactly("Ana Lima", "Bruno Alves", "Carlos Dias", "Diana Melo", "Eduardo Reis");
	}

	@Test
	void listaEspecialistasECirurgioesAtivosDaArea() {
		List<ProfissionalSaude> cardiologia = repository.findAtivosPorArea(AreaAtuacao.CARDIOLOGIA);

		assertThat(cardiologia).extracting(ProfissionalSaude::getNome).containsExactly("Ana Lima", "Bruno Alves");
		assertThat(cardiologia).hasExactlyElementsOfTypes(Cirurgiao.class, MedicoEspecialista.class);
	}

	@Test
	void clinicoGeralNaoApareceEmNenhumaArea() {
		assertThat(repository.findAtivosPorArea(AreaAtuacao.PEDIATRIA)).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(AreaAtuacao.class)
	void listagemPorAreaSegueOCriterioDoDominio(AreaAtuacao area) {
		List<String> esperados = repository.findByAtivoTrueOrderByNomeAsc().stream()
				.filter(profissional -> profissional.atendeArea(area))
				.map(ProfissionalSaude::getNome)
				.toList();

		assertThat(repository.findAtivosPorArea(area)).extracting(ProfissionalSaude::getNome)
				.containsExactlyElementsOf(esperados);
	}

}
