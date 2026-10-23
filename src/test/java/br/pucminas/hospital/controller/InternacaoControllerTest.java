package br.pucminas.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.dto.AltaRequest;
import br.pucminas.hospital.dto.InternacaoRequest;
import br.pucminas.hospital.exception.QuartoSemVagaException;
import br.pucminas.hospital.service.InternacaoService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternacaoController.class)
class InternacaoControllerTest {

	private static final String INTERNACAO = """
			{"pacienteId": 1, "profissionalId": 2, "quartoId": 3, "dataEntrada": "2030-02-01",
			 "dataPrevistaAlta": "2030-02-05"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private InternacaoService service;

	@Test
	void internaERespondeCreatedComLocation() throws Exception {
		when(service.internar(any(InternacaoRequest.class))).thenReturn(Exemplos.internacao(true));

		mvc.perform(post("/internacoes").contentType(MediaType.APPLICATION_JSON).content(INTERNACAO))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/internacoes/20"))
				.andExpect(jsonPath("$.quarto.numero").value("201"));
	}

	@Test
	void quartoSemVagaResponde422() throws Exception {
		when(service.internar(any(InternacaoRequest.class))).thenThrow(new QuartoSemVagaException("201"));

		mvc.perform(post("/internacoes").contentType(MediaType.APPLICATION_JSON).content(INTERNACAO))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("O quarto 201 não possui vagas disponíveis."));
	}

	@Test
	void listaAtivasPorPadraoOuEncerradas() throws Exception {
		when(service.listarAtivas()).thenReturn(List.of(Exemplos.internacao(true)));
		when(service.listarEncerradas()).thenReturn(List.of(Exemplos.internacao(false)));

		mvc.perform(get("/internacoes")).andExpect(jsonPath("$[0].ativa").value(true));
		mvc.perform(get("/internacoes").param("ativas", "false")).andExpect(jsonPath("$[0].ativa").value(false));
	}

	@Test
	void buscaPorId() throws Exception {
		when(service.buscarPorId(20L)).thenReturn(Exemplos.internacao(true));

		mvc.perform(get("/internacoes/20")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(20));
	}

	@Test
	void registraAlta() throws Exception {
		when(service.registrarAlta(eq(20L), any(AltaRequest.class))).thenReturn(Exemplos.internacao(false));

		mvc.perform(patch("/internacoes/20/registrar-alta").contentType(MediaType.APPLICATION_JSON)
				.content("{\"dataEfetivaAlta\": \"2020-02-04\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dataEfetivaAlta").value("2030-02-04"));
	}

	@Test
	void altaNoFuturoResponde400() throws Exception {
		mvc.perform(patch("/internacoes/20/registrar-alta").contentType(MediaType.APPLICATION_JSON)
				.content("{\"dataEfetivaAlta\": \"" + LocalDate.now().plusDays(1) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].mensagem").value("A data efetiva de alta não pode estar no futuro."));
		verify(service, never()).registrarAlta(any(), any());
	}

}
