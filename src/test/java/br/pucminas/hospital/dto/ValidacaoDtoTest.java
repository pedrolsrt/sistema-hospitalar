package br.pucminas.hospital.dto;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.CategoriaProfissional;
import br.pucminas.hospital.model.TipoQuarto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidacaoDtoTest {

	private static ValidatorFactory fabrica;
	private static Validator validator;

	@BeforeAll
	static void criarValidator() {
		fabrica = Validation.buildDefaultValidatorFactory();
		validator = fabrica.getValidator();
	}

	@AfterAll
	static void fecharValidator() {
		fabrica.close();
	}

	// Campo com problema -> mensagem.
	private static Map<String, String> erros(Object dto) {
		return validator.validate(dto).stream().collect(Collectors.toMap(
				violacao -> violacao.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a + " " + b));
	}

	private static PacienteRequest paciente(String cpf, LocalDate nascimento, String email) {
		return new PacienteRequest("Maria Silva", cpf, nascimento, null, email, null);
	}

	@ParameterizedTest
	@ValueSource(strings = { "52998224725", "529.982.247-25" })
	void aceitaCpfValidoComOuSemPontuacao(String cpf) {
		assertThat(erros(paciente(cpf, LocalDate.of(1990, 5, 10), "maria@email.com"))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "52998224726", "123", "abcdefghijk" })
	void recusaCpfInvalido(String cpf) {
		assertThat(erros(paciente(cpf, LocalDate.of(1990, 5, 10), null)))
				.containsEntry("cpf", "O CPF informado não é válido.");
	}

	@Test
	void recusaCamposObrigatoriosAusentes() {
		assertThat(erros(new PacienteRequest(" ", null, null, null, null, null)))
				.containsEntry("nome", "O nome é obrigatório.")
				.containsEntry("cpf", "O CPF é obrigatório.")
				.containsEntry("dataNascimento", "A data de nascimento é obrigatória.");
	}

	@Test
	void recusaNascimentoHojeOuNoFuturo() {
		assertThat(erros(paciente("52998224725", LocalDate.now(), null)))
				.containsEntry("dataNascimento", "A data de nascimento deve estar no passado.");
		assertThat(erros(paciente("52998224725", LocalDate.now().plusDays(1), null))).containsKey("dataNascimento");
	}

	@Test
	void recusaEmailInvalido() {
		assertThat(erros(paciente("52998224725", LocalDate.of(1990, 5, 10), "maria@")))
				.containsEntry("email", "O e-mail informado não é válido.");
	}

	@Test
	void validaOEnderecoAninhado() {
		PacienteRequest request = new PacienteRequest("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10), null,
				null, new EnderecoDto("", null, null, null, "Belo Horizonte", "MGG", "3014"));

		assertThat(erros(request)).containsOnlyKeys("endereco.logradouro", "endereco.uf", "endereco.cep");
	}

	@Test
	void areaObrigatoriaParaEspecialistaECirurgiao() {
		for (CategoriaProfissional categoria : new CategoriaProfissional[] { CategoriaProfissional.MEDICO_ESPECIALISTA,
				CategoriaProfissional.CIRURGIAO }) {
			ProfissionalSaudeRequest request = new ProfissionalSaudeRequest("Dra. Ana", "CRM-MG 1", categoria, null,
					null, null);
			assertThat(erros(request)).containsEntry("areaAtuacao",
					"Informe a área de atuação para a categoria " + categoria.getDescricao() + ".");
		}
	}

	@Test
	void clinicoGeralNaoTemArea() {
		ProfissionalSaudeRequest request = new ProfissionalSaudeRequest("Dra. Ana", "CRM-MG 1",
				CategoriaProfissional.CLINICO_GERAL, AreaAtuacao.PEDIATRIA, null, null);

		assertThat(erros(request)).containsEntry("areaAtuacao",
				"A categoria Clínico geral não tem área de atuação; não informe este campo.");
	}

	@Test
	void aceitaAreaCompativelComACategoria() {
		assertThat(erros(new ProfissionalSaudeRequest("Dra. Ana", "CRM-MG 1", CategoriaProfissional.CLINICO_GERAL, null,
				null, null))).isEmpty();
		assertThat(erros(new ProfissionalSaudeRequest("Dra. Ana", "CRM-MG 1", CategoriaProfissional.CIRURGIAO,
				AreaAtuacao.ORTOPEDIA, null, null))).isEmpty();
	}

	@Test
	void semCategoriaApontaSoACategoria() {
		assertThat(erros(new ProfissionalSaudeRequest("Dra. Ana", "CRM-MG 1", null, AreaAtuacao.ORTOPEDIA, null, null)))
				.containsOnlyKeys("categoria");
	}

	@Test
	void quartoExigeCamposECapacidadePositiva() {
		assertThat(erros(new QuartoRequest("", null, null, 0)))
				.containsOnlyKeys("numero", "andar", "tipo", "capacidadeMaxima");
		assertThat(erros(new QuartoRequest("101", 1, TipoQuarto.UTI, 1))).isEmpty();
	}

}
