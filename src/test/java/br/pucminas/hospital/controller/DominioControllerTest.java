package br.pucminas.hospital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.service.DominioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

// O service não acessa o banco, então é usado o real, para conferir os valores que chegam no JSON.
@WebMvcTest(DominioController.class)
@Import(DominioService.class)
class DominioControllerTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void listaOsValoresDeCadaEnumComCodigoEDescricao() throws Exception {
		mvc.perform(get("/dominios"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.situacoesConsulta.length()").value(6))
				.andExpect(jsonPath("$.situacoesConsulta[0].codigo").value("AGENDADA"))
				.andExpect(jsonPath("$.situacoesConsulta[0].descricao").value("Agendada"))
				.andExpect(jsonPath("$.situacoesEmergencia.length()").value(3))
				.andExpect(jsonPath("$.situacoesProcedimento.length()").value(4))
				.andExpect(jsonPath("$.situacoesInternacao[1].codigo").value("ENCERRADA"))
				.andExpect(jsonPath("$.modalidades.length()").value(5))
				.andExpect(jsonPath("$.categoriasProfissional[2].descricao").value("Cirurgião"))
				.andExpect(jsonPath("$.categoriasProfissional[0].exigeArea").value(false))
				.andExpect(jsonPath("$.categoriasProfissional[2].exigeArea").value(true))
				.andExpect(jsonPath("$.areasAtuacao.length()").value(19))
				.andExpect(jsonPath("$.tiposQuarto[2].codigo").value("UTI"))
				.andExpect(jsonPath("$.tiposQuarto[0].capacidadeMaximaPermitida").value(6))
				.andExpect(jsonPath("$.tiposQuarto[2].capacidadeMaximaPermitida").value(1))
				.andExpect(jsonPath("$.prioridadesEmergencia[3].descricao").value("Crítica"))
				.andExpect(jsonPath("$.tiposCirurgia.length()").value(3))
				.andExpect(jsonPath("$.ordenacoes[0].codigo").value("DATA"));
	}

}
