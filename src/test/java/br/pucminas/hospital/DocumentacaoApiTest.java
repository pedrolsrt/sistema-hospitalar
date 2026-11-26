package br.pucminas.hospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confere a documentação OpenAPI gerada pelo springdoc em /v3/api-docs: todos os recursos e operações da
 * API aparecem, com resumo, e as respostas de erro usam o esquema ProblemDetail.
 *
 * A lista de operações é fixa de propósito: uma rota nova, removida ou renomeada faz o teste falhar, para
 * que a tabela de endpoints do README seja revista junto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentacaoApiTest {

	private static final Set<String> RECURSOS = Set.of("Pacientes", "Profissionais de saúde", "Quartos",
			"Salas cirúrgicas", "Consultas", "Internações", "Emergências", "Procedimentos cirúrgicos", "Atendimentos",
			"Indicadores", "Domínios");

	private static final List<String> OPERACOES = List.of(
			"GET /pacientes", "POST /pacientes", "GET /pacientes/{id}", "PUT /pacientes/{id}",
			"PATCH /pacientes/{id}/inativar", "PATCH /pacientes/{id}/reativar", "GET /pacientes/{id}/historico",
			"GET /profissionais", "POST /profissionais", "GET /profissionais/{id}", "PUT /profissionais/{id}",
			"PATCH /profissionais/{id}/inativar", "PATCH /profissionais/{id}/reativar",
			"GET /profissionais/{id}/agenda", "GET /profissionais/{id}/procedimentos",
			"GET /quartos", "POST /quartos", "GET /quartos/{id}", "PUT /quartos/{id}", "DELETE /quartos/{id}",
			"GET /salas-cirurgicas", "POST /salas-cirurgicas", "GET /salas-cirurgicas/{id}",
			"PUT /salas-cirurgicas/{id}", "PATCH /salas-cirurgicas/{id}/inativar",
			"PATCH /salas-cirurgicas/{id}/reativar",
			"POST /consultas/rotina", "POST /consultas/especializadas", "GET /consultas", "GET /consultas/{id}",
			"PATCH /consultas/{id}/confirmar", "PATCH /consultas/{id}/iniciar", "PATCH /consultas/{id}/concluir",
			"PATCH /consultas/{id}/cancelar", "PATCH /consultas/{id}/registrar-nao-comparecimento",
			"PATCH /consultas/{id}/registrar-observacoes", "PATCH /consultas/{id}/encaminhar",
			"POST /internacoes", "GET /internacoes", "GET /internacoes/{id}",
			"PATCH /internacoes/{id}/registrar-alta", "PATCH /internacoes/{id}/registrar-observacoes",
			"POST /emergencias", "GET /emergencias", "GET /emergencias/fila", "GET /emergencias/{id}",
			"PATCH /emergencias/{id}/definir-prioridade", "PATCH /emergencias/{id}/iniciar",
			"PATCH /emergencias/{id}/concluir", "PATCH /emergencias/{id}/registrar-observacoes",
			"POST /procedimentos", "GET /procedimentos", "GET /procedimentos/{id}",
			"PATCH /procedimentos/{id}/iniciar", "PATCH /procedimentos/{id}/concluir",
			"PATCH /procedimentos/{id}/cancelar", "PATCH /procedimentos/{id}/registrar-observacoes-pre-operatorias",
			"PATCH /procedimentos/{id}/registrar-observacoes-pos-operatorias",
			"GET /atendimentos", "GET /indicadores", "GET /dominios");

	@Autowired
	private MockMvc mvc;

	private DocumentContext documentacao;

	@BeforeEach
	void carregarDocumentacao() throws Exception {
		String json = mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		documentacao = JsonPath.parse(json);
	}

	// Cada operação como mapa com método, rota, resumo, tags e respostas.
	private List<Map<String, Object>> operacoes() {
		Map<String, Map<String, Map<String, Object>>> caminhos = documentacao.read("$.paths");
		List<Map<String, Object>> operacoes = new ArrayList<>();
		caminhos.forEach((rota, metodos) -> metodos.forEach((metodo, operacao) -> {
			Map<String, Object> descricao = new HashMap<>(operacao);
			descricao.put("assinatura", metodo.toUpperCase() + " " + rota);
			operacoes.add(descricao);
		}));
		return operacoes;
	}

	@Test
	void documentacaoTemTituloEOsRecursosDaApi() {
		assertThat(documentacao.<String>read("$.info.title")).isEqualTo("Sistema de Informação Hospitalar");
		List<String> tags = documentacao.read("$.tags[*].name");
		assertThat(tags).containsExactlyInAnyOrderElementsOf(RECURSOS);
	}

	@Test
	void documentacaoTemTodasAsOperacoes() {
		assertThat(operacoes()).extracting(operacao -> (String) operacao.get("assinatura"))
				.containsExactlyInAnyOrderElementsOf(OPERACOES);
	}

	@Test
	@SuppressWarnings("unchecked")
	void todaOperacaoTemResumoERecursoDocumentados() {
		assertThat(operacoes()).allSatisfy(operacao -> {
			assertThat((String) operacao.get("summary")).as("resumo de %s", operacao.get("assinatura")).isNotBlank();
			assertThat((List<String>) operacao.get("tags")).as("recurso de %s", operacao.get("assinatura"))
					.hasSize(1)
					.allMatch(RECURSOS::contains);
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void respostasDeErroUsamOEsquemaProblema() {
		assertThat(documentacao.<Map<String, Object>>read("$.components.schemas.Problema")).isNotNull();
		assertThat(operacoes()).allSatisfy(operacao -> {
			Map<String, Map<String, Object>> respostas = (Map<String, Map<String, Object>>) operacao.get("responses");
			assertThat(respostas).as("respostas de %s", operacao.get("assinatura")).containsKey("500");
			respostas.forEach((codigo, resposta) -> {
				if (codigo.startsWith("4") || codigo.startsWith("5")) {
					String esquema = JsonPath.read(resposta, "$.content['application/problem+json'].schema.$ref");
					assertThat(esquema).as("esquema da resposta %s de %s", codigo, operacao.get("assinatura"))
							.isEqualTo("#/components/schemas/Problema");
				}
			});
		});
	}

}
