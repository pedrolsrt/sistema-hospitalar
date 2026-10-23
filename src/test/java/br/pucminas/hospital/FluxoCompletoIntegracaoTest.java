package br.pucminas.hospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Percorre a API de ponta a ponta, com a aplicação completa e o banco H2 migrado pelo Flyway.
 *
 * Usa um banco próprio e grava de verdade (sem rollback ao fim do teste), como a aplicação faz: assim os
 * mapeamentos e o carregamento preguiçoso são exercitados fora de uma transação de teste, e os dados não
 * interferem nos testes de repository, que usam o banco padrão do perfil test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:integracao;MODE=PostgreSQL;"
		+ "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1")
class FluxoCompletoIntegracaoTest {

	private static final LocalDate HOJE = LocalDate.now();

	@Autowired
	private MockMvc mvc;

	private String chamar(MockHttpServletRequestBuilder requisicao, String corpo, int statusEsperado) throws Exception {
		if (corpo != null) {
			requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
		}
		return mvc.perform(requisicao).andExpect(status().is(statusEsperado)).andReturn().getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
	}

	private long criar(String url, String corpo) throws Exception {
		return id(chamar(post(url), corpo, 201));
	}

	private static long id(String json) {
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private static String campo(String json, String caminho) {
		return JsonPath.read(json, caminho).toString();
	}

	@Test
	void fluxoCompletoDoHospital() throws Exception {
		// Cadastros --------------------------------------------------------------------------------------
		long maria = criar("/pacientes", """
				{"nome": "Maria Silva", "cpf": "529.982.247-25", "dataNascimento": "1990-05-10",
				 "email": "maria@email.com",
				 "endereco": {"logradouro": "Rua A", "numero": "10", "cidade": "Belo Horizonte", "uf": "MG",
				              "cep": "30140-071"}}
				""");
		chamar(post("/pacientes"), """
				{"nome": "Outra Maria", "cpf": "52998224725", "dataNascimento": "1980-01-01"}
				""", 409);

		long clinico = criar("/profissionais", """
				{"nome": "Dra. Ana Lima", "registroProfissional": "CRM-MG 1", "categoria": "CLINICO_GERAL"}
				""");
		long cardiologista = criar("/profissionais", """
				{"nome": "Dr. João Souza", "registroProfissional": "CRM-MG 2", "categoria": "MEDICO_ESPECIALISTA",
				 "areaAtuacao": "CARDIOLOGIA"}
				""");
		long cirurgiao = criar("/profissionais", """
				{"nome": "Dra. Carla Mendes", "registroProfissional": "CRM-MG 3", "categoria": "CIRURGIAO",
				 "areaAtuacao": "CIRURGIA_GERAL"}
				""");
		String cardiologia = chamar(get("/profissionais").param("area", "CARDIOLOGIA"), null, 200);
		assertThat(JsonPath.<List<String>>read(cardiologia, "$[*].nome")).containsExactly("Dr. João Souza");

		long enfermaria = criar("/quartos", "{\"numero\": \"101\", \"andar\": 1, \"tipo\": \"ENFERMARIA\", \"capacidadeMaxima\": 4}");
		long uti = criar("/quartos", "{\"numero\": \"201\", \"andar\": 2, \"tipo\": \"UTI\", \"capacidadeMaxima\": 1}");
		chamar(post("/quartos"), "{\"numero\": \"202\", \"andar\": 2, \"tipo\": \"UTI\", \"capacidadeMaxima\": 2}", 422);

		// Consulta de rotina até concluída ----------------------------------------------------------------
		long consulta = criar("/consultas/rotina", """
				{"pacienteId": %d, "profissionalId": %d, "data": "2099-01-15", "horario": "14:00", "motivo": "Check-up"}
				""".formatted(maria, clinico));
		chamar(patch("/consultas/" + consulta + "/confirmar"), null, 200);
		chamar(patch("/consultas/" + consulta + "/iniciar"), null, 200);
		chamar(patch("/consultas/" + consulta + "/registrar-observacoes"), "{\"texto\": \"Pressão 12x8.\"}", 200);
		String concluida = chamar(patch("/consultas/" + consulta + "/concluir"), null, 200);
		assertThat(campo(concluida, "$.situacao.codigo")).isEqualTo("CONCLUIDA");
		assertThat(campo(concluida, "$.observacoes")).isEqualTo("Pressão 12x8.");

		// Transição inválida: consulta concluída não pode ser cancelada.
		String transicao = chamar(patch("/consultas/" + consulta + "/cancelar"), null, 409);
		assertThat(campo(transicao, "$.detail")).isEqualTo("Não é possível cancelar uma consulta na situação Concluída. "
				+ "Essa situação é final e não pode mais ser alterada.");

		// Sobreposição de horário com uma consulta agendada do mesmo profissional.
		long retorno = criar("/consultas/rotina", """
				{"pacienteId": %d, "profissionalId": %d, "data": "2099-01-16", "horario": "10:00", "motivo": "Retorno"}
				""".formatted(maria, clinico));
		String sobreposta = chamar(post("/consultas/rotina"), """
				{"pacienteId": %d, "profissionalId": %d, "data": "2099-01-16", "horario": "10:15", "motivo": "Exames"}
				""".formatted(maria, clinico), 422);
		assertThat(campo(sobreposta, "$.detail")).startsWith("O profissional Dra. Ana Lima já tem consulta em 16/01/2099 às 10:00");
		String agenda = chamar(get("/profissionais/" + clinico + "/agenda").param("data", "2099-01-16"), null, 200);
		assertThat(JsonPath.<List<Integer>>read(agenda, "$[*].id")).containsExactly((int) retorno);

		// Consulta especializada com profissional de outra área (e com clínico geral) é recusada.
		chamar(post("/consultas/especializadas"), """
				{"pacienteId": %d, "profissionalId": %d, "data": "2099-01-20", "horario": "09:00", "motivo": "Cefaleia",
				 "areaAtuacao": "NEUROLOGIA"}
				""".formatted(maria, cardiologista), 422);
		chamar(post("/consultas/especializadas"), """
				{"pacienteId": %d, "profissionalId": %d, "data": "2099-01-20", "horario": "09:00", "motivo": "Dor no peito",
				 "areaAtuacao": "CARDIOLOGIA"}
				""".formatted(maria, clinico), 422);
		long especializada = criar("/consultas/especializadas", """
				{"pacienteId": %d, "profissionalId": %d, "data": "2099-01-20", "horario": "09:00", "motivo": "Dor no peito",
				 "areaAtuacao": "CARDIOLOGIA"}
				""".formatted(maria, cardiologista));

		// Internação e alta -------------------------------------------------------------------------------
		long internacao = criar("/internacoes", """
				{"pacienteId": %d, "profissionalId": %d, "quartoId": %d, "dataEntrada": "%s", "dataPrevistaAlta": "%s"}
				""".formatted(maria, clinico, uti, HOJE.minusDays(10), HOJE.minusDays(5)));
		assertThat(campo(chamar(get("/quartos/" + uti), null, 200), "$.situacao")).isEqualTo("OCUPADO");
		chamar(post("/internacoes"), """
				{"pacienteId": %d, "profissionalId": %d, "quartoId": %d, "dataEntrada": "%s", "dataPrevistaAlta": "%s"}
				""".formatted(maria, clinico, enfermaria, HOJE, HOJE.plusDays(2)), 422);
		chamar(patch("/pacientes/" + maria + "/inativar"), null, 422);
		chamar(patch("/internacoes/" + internacao + "/registrar-alta"),
				"{\"dataEfetivaAlta\": \"%s\"}".formatted(HOJE.minusDays(6)), 200);
		assertThat(campo(chamar(get("/quartos/" + uti), null, 200), "$.ocupacaoAtual")).isEqualTo("0");
		chamar(delete("/quartos/" + uti), null, 422);
		chamar(delete("/quartos/" + enfermaria), null, 204);
		chamar(get("/quartos/" + enfermaria), null, 404);

		// Emergência pela fila até concluir ---------------------------------------------------------------
		long emergencia = criar("/emergencias", "{\"pacienteId\": %d, \"profissionalId\": %d}".formatted(maria, clinico));
		String fila = chamar(get("/emergencias/fila"), null, 200);
		assertThat(JsonPath.<List<Integer>>read(fila, "$[*].id")).containsExactly((int) emergencia);
		chamar(patch("/emergencias/" + emergencia + "/iniciar"), null, 422);
		chamar(patch("/emergencias/" + emergencia + "/definir-prioridade"), "{\"prioridade\": \"ALTA\"}", 200);
		chamar(patch("/emergencias/" + emergencia + "/iniciar"), null, 200);
		chamar(patch("/emergencias/" + emergencia + "/concluir"), null, 200);
		assertThat(JsonPath.<List<Object>>read(chamar(get("/emergencias/fila"), null, 200), "$")).isEmpty();

		// Procedimento até concluído ----------------------------------------------------------------------
		chamar(post("/procedimentos"), """
				{"pacienteId": %d, "cirurgiaoId": %d, "descricao": "Apendicectomia", "tipoCirurgia": "URGENCIA",
				 "dataHoraPrevista": "2099-02-01T08:00:00"}
				""".formatted(maria, clinico), 422);
		long procedimento = criar("/procedimentos", """
				{"pacienteId": %d, "cirurgiaoId": %d, "descricao": "Apendicectomia", "tipoCirurgia": "URGENCIA",
				 "dataHoraPrevista": "2099-02-01T08:00:00", "observacoesPreOperatorias": "Jejum de 8 horas."}
				""".formatted(maria, cirurgiao));
		chamar(patch("/procedimentos/" + procedimento + "/concluir"), "{\"dataHoraFim\": \"2099-02-01T10:00:00\"}", 409);
		chamar(patch("/procedimentos/" + procedimento + "/iniciar"), "{\"dataHoraInicio\": \"2099-02-01T08:10:00\"}", 200);
		chamar(patch("/procedimentos/" + procedimento + "/registrar-observacoes-pos-operatorias"),
				"{\"texto\": \"Sem intercorrências.\"}", 200);
		chamar(patch("/procedimentos/" + procedimento + "/concluir"), "{\"dataHoraFim\": \"2099-02-01T10:00:00\"}", 200);
		String realizados = chamar(get("/profissionais/" + cirurgiao + "/procedimentos"), null, 200);
		assertThat(JsonPath.<List<Integer>>read(realizados, "$[*].id")).containsExactly((int) procedimento);

		// Histórico do paciente em ordem cronológica -------------------------------------------------------
		String historico = chamar(get("/pacientes/" + maria + "/historico"), null, 200);
		assertThat(JsonPath.<List<String>>read(historico, "$.atendimentos[*].modalidade.codigo")).containsExactly(
				"INTERNACAO", "EMERGENCIA", "CONSULTA_ROTINA", "CONSULTA_ROTINA", "CONSULTA_ESPECIALIZADA",
				"PROCEDIMENTO_CIRURGICO");
		List<String> datas = JsonPath.read(historico, "$.atendimentos[*].dataHora");
		assertThat(datas).isSorted();
		assertThat(JsonPath.<List<Integer>>read(historico, "$.atendimentos[*].detalhes.id")).containsExactly(
				(int) internacao, (int) emergencia, (int) consulta, (int) retorno, (int) especializada, (int) procedimento);
		assertThat(campo(historico, "$.atendimentos[0].detalhes.quarto.numero")).isEqualTo("201");
		assertThat(campo(historico, "$.atendimentos[4].detalhes.areaAtuacao.descricao")).isEqualTo("Cardiologia");
		assertThat(campo(historico, "$.atendimentos[5].detalhes.observacoesPosOperatorias"))
				.isEqualTo("Sem intercorrências.");

		// Com a consulta de retorno ainda agendada, o paciente não pode ser inativado; depois de cancelá-la, pode.
		String impedido = chamar(patch("/pacientes/" + maria + "/inativar"), null, 422);
		assertThat(campo(impedido, "$.detail")).isEqualTo("O paciente Maria Silva não pode ser inativado porque tem: "
				+ "consulta agendada, confirmada ou em atendimento.");
		chamar(patch("/consultas/" + retorno + "/cancelar"), null, 200);
		chamar(patch("/consultas/" + especializada + "/cancelar"), null, 200);
		assertThat(campo(chamar(patch("/pacientes/" + maria + "/inativar"), null, 200), "$.ativo")).isEqualTo("false");
	}

}
