package br.pucminas.hospital.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.dto.AtendimentosPorModalidadeResponse;
import br.pucminas.hospital.dto.AtendimentosPorProfissionalResponse;
import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.IndicadoresResponse;
import br.pucminas.hospital.dto.QuartosPorSituacaoResponse;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.service.IndicadorService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IndicadorController.class)
class IndicadorControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private IndicadorService service;

	private static IndicadoresResponse indicadores(LocalDate inicio, LocalDate fim) {
		return new IndicadoresResponse(inicio, fim, 3,
				List.of(new AtendimentosPorModalidadeResponse(new CodigoDescricao("CONSULTA_ROTINA", "Consulta de rotina"),
						4)),
				new QuartosPorSituacaoResponse(5, 1),
				List.of(new AtendimentosPorProfissionalResponse(1L, "Dra. Ana Lima",
						new CodigoDescricao("CLINICO_GERAL", "Clínico geral"), 4)));
	}

	@Test
	void semPeriodoCalculaComTudo() throws Exception {
		when(service.calcular(null, null)).thenReturn(indicadores(null, null));

		mvc.perform(get("/indicadores"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inicio").doesNotExist())
				.andExpect(jsonPath("$.consultasRealizadas").value(3))
				.andExpect(jsonPath("$.atendimentosPorModalidade[0].modalidade.codigo").value("CONSULTA_ROTINA"))
				.andExpect(jsonPath("$.atendimentosPorModalidade[0].modalidade.descricao").value("Consulta de rotina"))
				.andExpect(jsonPath("$.atendimentosPorModalidade[0].quantidade").value(4))
				.andExpect(jsonPath("$.quartos.disponiveis").value(5))
				.andExpect(jsonPath("$.quartos.ocupados").value(1))
				.andExpect(jsonPath("$.atendimentosPorProfissional[0].nome").value("Dra. Ana Lima"))
				.andExpect(jsonPath("$.atendimentosPorProfissional[0].categoria.descricao").value("Clínico geral"))
				.andExpect(jsonPath("$.atendimentosPorProfissional[0].quantidade").value(4));
	}

	@Test
	void repassaOPeriodo() throws Exception {
		LocalDate inicio = LocalDate.of(2030, 1, 1);
		LocalDate fim = LocalDate.of(2030, 1, 31);
		when(service.calcular(inicio, fim)).thenReturn(indicadores(inicio, fim));

		mvc.perform(get("/indicadores").param("inicio", "2030-01-01").param("fim", "2030-01-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inicio").value("2030-01-01"))
				.andExpect(jsonPath("$.fim").value("2030-01-31"));
	}

	@Test
	void inicioPosteriorAoFimResponde422() throws Exception {
		LocalDate inicio = LocalDate.of(2030, 2, 1);
		LocalDate fim = LocalDate.of(2030, 1, 31);
		when(service.calcular(inicio, fim)).thenThrow(
				new DataInvalidaException("O início do período (01/02/2030) não pode ser posterior ao fim (31/01/2030)."));

		mvc.perform(get("/indicadores").param("inicio", "2030-02-01").param("fim", "2030-01-31"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail")
						.value("O início do período (01/02/2030) não pode ser posterior ao fim (31/01/2030)."));
	}

	@Test
	void dataEmFormatoInvalidoResponde400() throws Exception {
		mvc.perform(get("/indicadores").param("inicio", "01/02/2030"))
				.andExpect(status().isBadRequest());
	}

}
