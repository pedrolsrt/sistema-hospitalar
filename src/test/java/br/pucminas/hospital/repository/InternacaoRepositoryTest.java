package br.pucminas.hospital.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.Internacao;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
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
class InternacaoRepositoryTest {

	private static final LocalDate DIA = LocalDate.of(2030, 2, 1);

	@Autowired
	private TestEntityManager em;

	@Autowired
	private InternacaoRepository repository;

	private Paciente maria;
	private Paciente jose;
	private Paciente carla;
	private Quarto enfermaria;
	private Quarto uti;
	private Quarto apartamentoComAlta;
	private Quarto apartamentoVazio;

	@BeforeEach
	void cadastrar() {
		maria = em.persist(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)));
		jose = em.persist(new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1)));
		carla = em.persist(new Paciente("Carla Dias", "39053344705", LocalDate.of(1970, 9, 30)));
		ProfissionalSaude medico = em.persist(new ClinicoGeral("Dr. João Souza", "CRM-MG 1"));
		enfermaria = em.persist(new Quarto("101", 1, TipoQuarto.ENFERMARIA, 4));
		uti = em.persist(new Quarto("201", 2, TipoQuarto.UTI, 1));
		apartamentoComAlta = em.persist(new Quarto("301", 3, TipoQuarto.APARTAMENTO, 2));
		apartamentoVazio = em.persist(new Quarto("302", 3, TipoQuarto.APARTAMENTO, 2));

		em.persist(new Internacao(maria, medico, enfermaria, DIA, DIA.plusDays(5)));
		em.persist(new Internacao(carla, medico, uti, DIA.minusDays(2), DIA.plusDays(1)));
		Internacao encerrada = new Internacao(jose, medico, apartamentoComAlta, DIA.minusDays(10), DIA.minusDays(5));
		encerrada.darAlta(DIA.minusDays(4));
		em.persist(encerrada);
		em.flush();
		em.clear();
	}

	@Test
	void internacoesAtivasEmOrdemDeEntrada() {
		assertThat(repository.findByDataEfetivaAltaIsNullOrderByDataEntradaAsc())
				.extracting(internacao -> internacao.getPaciente().getNome())
				.containsExactly("Carla Dias", "Maria Silva");
	}

	@Test
	void internacaoAtivaDoPaciente() {
		assertThat(repository.findByPacienteIdAndDataEfetivaAltaIsNull(maria.getId()))
				.get().extracting(internacao -> internacao.getQuarto().getNumero()).isEqualTo("101");
		assertThat(repository.findByPacienteIdAndDataEfetivaAltaIsNull(jose.getId())).isEmpty();
	}

	@Test
	void verificaQuartoComInternacaoAtiva() {
		assertThat(repository.existsByQuartoIdAndDataEfetivaAltaIsNull(enfermaria.getId())).isTrue();
		assertThat(repository.existsByQuartoIdAndDataEfetivaAltaIsNull(uti.getId())).isTrue();
		assertThat(repository.existsByQuartoIdAndDataEfetivaAltaIsNull(apartamentoComAlta.getId())).isFalse();
		assertThat(repository.existsByQuartoIdAndDataEfetivaAltaIsNull(apartamentoVazio.getId())).isFalse();
	}

	@Test
	void verificaQuartoComQualquerInternacao() {
		assertThat(repository.existsByQuartoId(enfermaria.getId())).isTrue();
		assertThat(repository.existsByQuartoId(apartamentoComAlta.getId())).isTrue();
		assertThat(repository.existsByQuartoId(apartamentoVazio.getId())).isFalse();
	}

}
