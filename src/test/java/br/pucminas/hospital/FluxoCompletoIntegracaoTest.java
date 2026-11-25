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
		chamar(patch("/consultas/" + consulta + "/encaminhar"), "{\"areaAtuacao\": \"CARDIOLOGIA\"}", 200);
		String encaminhada = chamar(get("/consultas/" + consulta), null, 200);
		assertThat(campo(encaminhada, "$.areaEncaminhamento.codigo")).isEqualTo("CARDIOLOGIA");

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
				""".formatted(maria, clinico), 409);
		assertThat(campo(sobreposta, "$.title")).isEqualTo("Horário indisponível");
		assertThat(campo(sobreposta, "$.detail"))
				.startsWith("O profissional Dra. Ana Lima já tem consulta em 16/01/2099, das 10:00 às 10:30");
		String agenda = chamar(get("/profissionais/" + clinico + "/agenda").param("data", "2099-01-16"), null, 200);
		assertThat(JsonPath.<List<Integer>>read(agenda, "$[*].id")).containsExactly((int) retorno);
		chamar(get("/profissionais/999/agenda").param("data", "2099-01-16"), null, 404);

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
				{"pacienteId": %d, "profissionalId": %d, "quartoId": %d, "dataEntrada": "%s", "dataPrevistaAlta": "%s",
				 "observacoes": "Dieta leve."}
				""".formatted(maria, clinico, uti, HOJE.minusDays(10), HOJE.minusDays(5)));
		assertThat(campo(chamar(get("/internacoes/" + internacao), null, 200), "$.observacoes"))
				.isEqualTo("Dieta leve.");
		assertThat(campo(chamar(get("/quartos/" + uti), null, 200), "$.situacao")).isEqualTo("OCUPADO");
		chamar(post("/internacoes"), """
				{"pacienteId": %d, "profissionalId": %d, "quartoId": %d, "dataEntrada": "%s", "dataPrevistaAlta": "%s"}
				""".formatted(maria, clinico, enfermaria, HOJE, HOJE.plusDays(2)), 422);
		chamar(patch("/pacientes/" + maria + "/inativar"), null, 422);
		chamar(patch("/internacoes/" + internacao + "/registrar-alta"),
				"{\"dataEfetivaAlta\": \"%s\"}".formatted(HOJE.minusDays(6)), 200);
		assertThat(campo(chamar(get("/quartos/" + uti), null, 200), "$.ocupacaoAtual")).isEqualTo("0");
		String observacoesDaAlta = chamar(patch("/internacoes/" + internacao + "/registrar-observacoes"),
				"{\"texto\": \"Retorno em 15 dias.\"}", 200);
		assertThat(campo(observacoesDaAlta, "$.observacoes")).isEqualTo("Retorno em 15 dias.");
		chamar(delete("/quartos/" + uti), null, 422);
		chamar(delete("/quartos/" + enfermaria), null, 204);
		chamar(get("/quartos/" + enfermaria), null, 404);

		// Emergência pela fila até concluir ---------------------------------------------------------------
		long emergencia = criar("/emergencias", "{\"pacienteId\": %d, \"profissionalId\": %d}".formatted(maria, clinico));
		String fila = chamar(get("/emergencias/fila"), null, 200);
		assertThat(JsonPath.<List<Integer>>read(fila, "$[*].id")).containsExactly((int) emergencia);
		chamar(patch("/emergencias/" + emergencia + "/iniciar"), null, 422);
		chamar(patch("/emergencias/" + emergencia + "/registrar-observacoes"), "{\"texto\": \"Sutura.\"}", 422);
		chamar(patch("/emergencias/" + emergencia + "/definir-prioridade"), "{\"prioridade\": \"ALTA\"}", 200);
		chamar(patch("/emergencias/" + emergencia + "/iniciar"), null, 200);
		String observacoesDaEmergencia = chamar(patch("/emergencias/" + emergencia + "/registrar-observacoes"),
				"{\"texto\": \"Sutura no antebraço.\"}", 200);
		assertThat(campo(observacoesDaEmergencia, "$.observacoes")).isEqualTo("Sutura no antebraço.");
		chamar(patch("/emergencias/" + emergencia + "/concluir"), null, 200);
		assertThat(JsonPath.<List<Object>>read(chamar(get("/emergencias/fila"), null, 200), "$")).isEmpty();

		// Salas cirúrgicas --------------------------------------------------------------------------------
		long sala1 = criar("/salas-cirurgicas", "{\"identificacao\": \"Sala 1\"}");
		chamar(post("/salas-cirurgicas"), "{\"identificacao\": \"Sala 1\"}", 409);
		long sala2 = criar("/salas-cirurgicas", "{\"identificacao\": \"Sala 2\"}");
		chamar(patch("/salas-cirurgicas/" + sala2 + "/inativar"), null, 200);
		String ativas = chamar(get("/salas-cirurgicas").param("ativas", "true"), null, 200);
		assertThat(JsonPath.<List<String>>read(ativas, "$[*].identificacao")).containsExactly("Sala 1");

		// Procedimento até concluído ----------------------------------------------------------------------
		chamar(post("/procedimentos"), """
				{"pacienteId": %d, "cirurgiaoId": %d, "descricao": "Apendicectomia", "tipoCirurgia": "URGENCIA",
				 "salaCirurgicaId": %d, "dataHoraPrevista": "2099-02-01T08:00:00", "duracaoPrevistaMinutos": 120}
				""".formatted(maria, clinico, sala1), 422);
		chamar(post("/procedimentos"), """
				{"pacienteId": %d, "cirurgiaoId": %d, "descricao": "Apendicectomia", "tipoCirurgia": "URGENCIA",
				 "salaCirurgicaId": %d, "dataHoraPrevista": "2099-02-01T08:00:00", "duracaoPrevistaMinutos": 120}
				""".formatted(maria, cirurgiao, sala2), 422);
		long procedimento = criar("/procedimentos", """
				{"pacienteId": %d, "cirurgiaoId": %d, "descricao": "Apendicectomia", "tipoCirurgia": "URGENCIA",
				 "salaCirurgicaId": %d, "dataHoraPrevista": "2099-02-01T08:00:00", "duracaoPrevistaMinutos": 120,
				 "observacoesPreOperatorias": "Jejum de 8 horas."}
				""".formatted(maria, cirurgiao, sala1));
		assertThat(campo(chamar(get("/procedimentos/" + procedimento), null, 200), "$.dataHoraFimPrevista"))
				.isEqualTo("2099-02-01T10:00:00");

		// Disponibilidade: a sala, o cirurgião e o paciente ficam ocupados das 08:00 às 10:00.
		long jose = criar("/pacientes", """
				{"nome": "José Lima", "cpf": "111.444.777-35", "dataNascimento": "1985-02-01"}
				""");
		long ortopedista = criar("/profissionais", """
				{"nome": "Dr. Paulo Reis", "registroProfissional": "CRM-MG 4", "categoria": "CIRURGIAO",
				 "areaAtuacao": "ORTOPEDIA"}
				""");
		String salaOcupada = chamar(post("/procedimentos"), """
				{"pacienteId": %d, "cirurgiaoId": %d, "descricao": "Artroscopia", "tipoCirurgia": "ELETIVA",
				 "salaCirurgicaId": %d, "dataHoraPrevista": "2099-02-01T09:00:00", "duracaoPrevistaMinutos": 60}
				""".formatted(jose, ortopedista, sala1), 409);
		assertThat(campo(salaOcupada, "$.detail")).isEqualTo("A sala cirúrgica Sala 1 já tem procedimento cirúrgico "
				+ "em 01/02/2099, das 08:00 às 10:00, que se sobrepõe ao período pedido "
				+ "(01/02/2099, das 09:00 às 10:00).");
		String cirurgiaoOcupado = chamar(post("/consultas/rotina"), """
				{"pacienteId": %d, "profissionalId": %d, "data": "2099-02-01", "horario": "09:30",
				 "motivo": "Avaliação"}
				""".formatted(jose, cirurgiao), 409);
		assertThat(campo(cirurgiaoOcupado, "$.detail"))
				.startsWith("O profissional Dra. Carla Mendes já tem procedimento cirúrgico em 01/02/2099");
		criar("/procedimentos", """
				{"pacienteId": %d, "cirurgiaoId": %d, "descricao": "Artroscopia", "tipoCirurgia": "ELETIVA",
				 "salaCirurgicaId": %d, "dataHoraPrevista": "2099-02-01T10:00:00", "duracaoPrevistaMinutos": 60}
				""".formatted(jose, ortopedista, sala1));
		chamar(patch("/salas-cirurgicas/" + sala1 + "/inativar"), null, 422);
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

		// Busca de atendimentos com filtros e ordenação ------------------------------------------------------
		String procedimentosDaMaria = chamar(get("/atendimentos").param("pacienteId", String.valueOf(maria))
				.param("modalidade", "PROCEDIMENTO_CIRURGICO"), null, 200);
		assertThat(JsonPath.<List<Integer>>read(procedimentosDaMaria, "$[*].detalhes.id"))
				.containsExactly((int) procedimento);
		assertThat(campo(procedimentosDaMaria, "$[0].situacao.codigo")).isEqualTo("CONCLUIDO");
		String encerradas = chamar(get("/atendimentos").param("situacao", "ENCERRADA"), null, 200);
		assertThat(JsonPath.<List<Integer>>read(encerradas, "$[*].detalhes.id")).containsExactly((int) internacao);
		String maisRecentes = chamar(get("/atendimentos").param("pacienteId", String.valueOf(maria))
				.param("ordem", "DATA_DECRESCENTE"), null, 200);
		assertThat(JsonPath.<List<String>>read(maisRecentes, "$[*].modalidade.codigo")).containsExactly(
				"PROCEDIMENTO_CIRURGICO", "CONSULTA_ESPECIALIZADA", "CONSULTA_ROTINA", "CONSULTA_ROTINA", "EMERGENCIA",
				"INTERNACAO");
		String concluidas = chamar(get("/consultas").param("situacao", "CONCLUIDA"), null, 200);
		assertThat(JsonPath.<List<Integer>>read(concluidas, "$[*].id")).containsExactly((int) consulta);
		chamar(get("/atendimentos").param("inicio", "2099-02-01").param("fim", "2099-01-31"), null, 422);
		String situacaoDesconhecida = chamar(get("/atendimentos").param("situacao", "FINALIZADA"), null, 400);
		assertThat(campo(situacaoDesconhecida, "$.campos[0].mensagem"))
				.startsWith("A situação informada não foi reconhecida. Use um destes códigos: AGENDADA");

		// Indicadores conferidos com o que a busca de atendimentos e a lista de quartos devolvem -----------
		String indicadores = chamar(get("/indicadores"), null, 200);
		String todos = chamar(get("/atendimentos"), null, 200);
		assertThat(JsonPath.<Integer>read(indicadores, "$.consultasRealizadas")).isEqualTo(1);
		for (String modalidade : List.of("CONSULTA_ROTINA", "CONSULTA_ESPECIALIZADA", "EMERGENCIA",
				"PROCEDIMENTO_CIRURGICO", "INTERNACAO")) {
			assertThat(JsonPath.<List<Integer>>read(indicadores,
					"$.atendimentosPorModalidade[?(@.modalidade.codigo == '" + modalidade + "')].quantidade"))
					.containsExactly(JsonPath.<List<Object>>read(todos,
							"$[?(@.modalidade.codigo == '" + modalidade + "')]").size());
		}
		assertThat(JsonPath.<List<Integer>>read(indicadores, "$.atendimentosPorProfissional[*].quantidade")
				.stream().mapToInt(Integer::intValue).sum()).isEqualTo(JsonPath.<List<Object>>read(todos, "$").size());
		String quartos = chamar(get("/quartos"), null, 200);
		assertThat(JsonPath.<Integer>read(indicadores, "$.quartos.ocupados"))
				.isEqualTo(JsonPath.<List<Object>>read(quartos, "$[?(@.situacao == 'OCUPADO')]").size());
		assertThat(JsonPath.<Integer>read(indicadores, "$.quartos.disponiveis"))
				.isEqualTo(JsonPath.<List<Object>>read(quartos, "$[?(@.situacao == 'DISPONIVEL')]").size());
		String diaDosProcedimentos = chamar(get("/indicadores").param("inicio", "2099-02-01").param("fim", "2099-02-01"),
				null, 200);
		assertThat(JsonPath.<List<Integer>>read(diaDosProcedimentos,
				"$.atendimentosPorModalidade[?(@.modalidade.codigo == 'PROCEDIMENTO_CIRURGICO')].quantidade"))
				.containsExactly(JsonPath.<List<Object>>read(chamar(get("/atendimentos")
						.param("modalidade", "PROCEDIMENTO_CIRURGICO").param("inicio", "2099-02-01")
						.param("fim", "2099-02-01"), null, 200), "$").size());
		chamar(get("/indicadores").param("inicio", "2099-02-01").param("fim", "2099-01-31"), null, 422);
		String dominios = chamar(get("/dominios"), null, 200);
		assertThat(JsonPath.<List<String>>read(dominios, "$.areasAtuacao[?(@.codigo == 'CIRURGIA_GERAL')].descricao"))
				.containsExactly("Cirurgia geral");

		// Com a consulta de retorno ainda agendada, o paciente não pode ser inativado; depois de cancelá-la, pode.
		String impedido = chamar(patch("/pacientes/" + maria + "/inativar"), null, 422);
		assertThat(campo(impedido, "$.detail")).isEqualTo("O paciente Maria Silva não pode ser inativado porque tem: "
				+ "consulta agendada, confirmada ou em atendimento.");
		chamar(patch("/consultas/" + retorno + "/cancelar"), null, 200);
		chamar(patch("/consultas/" + especializada + "/cancelar"), null, 200);
		assertThat(campo(chamar(patch("/pacientes/" + maria + "/inativar"), null, 200), "$.ativo")).isEqualTo("false");
	}

}
