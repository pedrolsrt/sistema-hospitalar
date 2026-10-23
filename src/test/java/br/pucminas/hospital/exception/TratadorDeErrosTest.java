package br.pucminas.hospital.exception;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.dto.PacienteRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeRequest;
import jakarta.validation.Valid;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class TratadorDeErrosTest {

	// Controller só de teste: cada endpoint provoca um tipo de erro.
	@RestController
	static class ControllerDeTeste {

		@GetMapping("/nao-encontrado")
		void naoEncontrado() {
			throw new RecursoNaoEncontradoException("Paciente não encontrado (id 9).");
		}

		@GetMapping("/duplicado")
		void duplicado() {
			throw new DadoJaCadastradoException("Já existe um quarto com o número 101.");
		}

		@GetMapping("/transicao")
		void transicao() {
			throw new TransicaoSituacaoInvalidaException("Não é possível confirmar uma consulta na situação Cancelada.");
		}

		@GetMapping("/regra")
		void regra() {
			throw new QuartoSemVagaException("101");
		}

		@GetMapping("/integridade")
		void integridade() {
			throw new DataIntegrityViolationException("duplicate key value violates unique constraint uk_paciente_cpf",
					new SQLException("detalhe interno do banco"));
		}

		@GetMapping("/concorrencia")
		void concorrencia() {
			throw new ObjectOptimisticLockingFailureException("br.pucminas.hospital.model.Quarto", 1L);
		}

		@GetMapping("/inesperado")
		void inesperado() {
			throw new IllegalStateException("NullPointer em ServicoInterno.linha42");
		}

		@GetMapping("/quartos/{id}")
		String quarto(@PathVariable Long id) {
			return "ok";
		}

		@GetMapping("/busca")
		String busca(@RequestParam String nome) {
			return nome;
		}

		@PostMapping("/pacientes")
		String paciente(@Valid @RequestBody PacienteRequest request) {
			return "ok";
		}

		@PostMapping("/profissionais")
		String profissional(@Valid @RequestBody ProfissionalSaudeRequest request) {
			return "ok";
		}

	}

	private MockMvc mvc;

	@BeforeEach
	void configurar() {
		mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste()).setControllerAdvice(new TratadorDeErros()).build();
	}

	@Test
	void recursoNaoEncontradoResponde404() throws Exception {
		mvc.perform(get("/nao-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso não encontrado"))
				.andExpect(jsonPath("$.detail").value("Paciente não encontrado (id 9)."));
	}

	@Test
	void dadoJaCadastradoResponde409() throws Exception {
		mvc.perform(get("/duplicado"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Dado já cadastrado"))
				.andExpect(jsonPath("$.detail").value("Já existe um quarto com o número 101."));
	}

	@Test
	void transicaoInvalidaResponde409() throws Exception {
		mvc.perform(get("/transicao"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Transição de situação inválida"));
	}

	@Test
	void demaisRegrasDeNegocioRespondem422() throws Exception {
		mvc.perform(get("/regra"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.title").value("Regra de negócio violada"))
				.andExpect(jsonPath("$.detail").value("O quarto 101 não possui vagas disponíveis."));
	}

	@Test
	void violacaoDeIntegridadeResponde409SemExporOBanco() throws Exception {
		mvc.perform(get("/integridade"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Conflito de dados"))
				.andExpect(jsonPath("$.detail", not(containsString("uk_paciente_cpf"))))
				.andExpect(jsonPath("$.detail", not(containsString("detalhe interno"))));
	}

	@Test
	void conflitoDeVersaoResponde409() throws Exception {
		mvc.perform(get("/concorrencia"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Alteração simultânea"))
				.andExpect(jsonPath("$.detail", containsString("alterados por outra operação")));
	}

	@Test
	void erroInesperadoResponde500SemDetalhesInternos() throws Exception {
		mvc.perform(get("/inesperado"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
				.andExpect(content().string(not(containsString("ServicoInterno"))));
	}

	@Test
	void dadosInvalidosRespondem400ListandoOsCampos() throws Exception {
		String corpo = """
				{"nome": "", "cpf": "11111111112", "dataNascimento": "2999-01-01", "email": "maria@"}
				""";

		mvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.campos[*].campo")
						.value(containsInAnyOrder("cpf", "dataNascimento", "email", "nome")))
				.andExpect(jsonPath("$.campos[?(@.campo == 'cpf')].mensagem").value("O CPF informado não é válido."));
	}

	@Test
	void areaIncompativelComACategoriaApontaOCampoAreaAtuacao() throws Exception {
		String corpo = """
				{"nome": "Dr. João", "registroProfissional": "CRM-MG 1", "categoria": "MEDICO_ESPECIALISTA"}
				""";

		mvc.perform(post("/profissionais").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("areaAtuacao"))
				.andExpect(jsonPath("$.campos[0].mensagem")
						.value("Informe a área de atuação para a categoria Médico especialista."));
	}

	@Test
	void corpoMalFormadoOuComValorNaoAceitoResponde400() throws Exception {
		mvc.perform(post("/profissionais").contentType(MediaType.APPLICATION_JSON).content("{\"nome\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Requisição inválida"));
		mvc.perform(post("/profissionais").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"Dr. João\", \"registroProfissional\": \"CRM\", \"categoria\": \"DENTISTA\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O corpo da requisição está mal formatado ou contém um valor não aceito."));
	}

	@Test
	void parametroDeTipoErradoResponde400() throws Exception {
		mvc.perform(get("/quartos/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O valor 'abc' não é aceito para o parâmetro id."));
	}

	@Test
	void parametroObrigatorioAusenteResponde400() throws Exception {
		mvc.perform(get("/busca"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O parâmetro nome é obrigatório."));
	}

	@Test
	void metodoNaoSuportadoResponde405() throws Exception {
		mvc.perform(delete("/busca"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.title").value("Método não permitido"));
	}

}
