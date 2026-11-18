package br.pucminas.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.dto.SalaCirurgicaRequest;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.service.SalaCirurgicaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SalaCirurgicaController.class)
class SalaCirurgicaControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private SalaCirurgicaService service;

	@Test
	void listaTodasPorPadraoOuSoAsAtivas() throws Exception {
		when(service.listar(false)).thenReturn(List.of(Exemplos.sala(true), Exemplos.sala(false)));
		when(service.listar(true)).thenReturn(List.of(Exemplos.sala(true)));

		mvc.perform(get("/salas-cirurgicas")).andExpect(jsonPath("$.length()").value(2));
		mvc.perform(get("/salas-cirurgicas").param("ativas", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].identificacao").value("Sala 1"))
				.andExpect(jsonPath("$[0].ativa").value(true));
	}

	@Test
	void cadastraERespondeCreatedComLocation() throws Exception {
		when(service.cadastrar(any(SalaCirurgicaRequest.class))).thenReturn(Exemplos.sala(true));

		mvc.perform(post("/salas-cirurgicas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"identificacao\": \"Sala 1\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/salas-cirurgicas/5"));
	}

	@Test
	void identificacaoAusenteOuLongaDemaisResponde400() throws Exception {
		mvc.perform(post("/salas-cirurgicas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"identificacao\": \" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].mensagem").value("A identificação da sala é obrigatória."));
		mvc.perform(post("/salas-cirurgicas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"identificacao\": \"" + "S".repeat(31) + "\"}"))
				.andExpect(status().isBadRequest());
		verify(service, never()).cadastrar(any());
	}

	@Test
	void identificacaoJaCadastradaResponde409() throws Exception {
		when(service.cadastrar(any(SalaCirurgicaRequest.class)))
				.thenThrow(new DadoJaCadastradoException("Já existe uma sala cirúrgica com a identificação Sala 1."));

		mvc.perform(post("/salas-cirurgicas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"identificacao\": \"Sala 1\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void buscaEAtualiza() throws Exception {
		when(service.buscarPorId(5L)).thenReturn(Exemplos.sala(true));
		when(service.atualizar(eq(5L), any(SalaCirurgicaRequest.class))).thenReturn(Exemplos.sala(true));

		mvc.perform(get("/salas-cirurgicas/5")).andExpect(jsonPath("$.identificacao").value("Sala 1"));
		mvc.perform(put("/salas-cirurgicas/5").contentType(MediaType.APPLICATION_JSON)
				.content("{\"identificacao\": \"Sala 1\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void salaInexistenteResponde404() throws Exception {
		when(service.buscarPorId(99L))
				.thenThrow(new RecursoNaoEncontradoException("Sala cirúrgica não encontrada (id 99)."));

		mvc.perform(get("/salas-cirurgicas/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Sala cirúrgica não encontrada (id 99)."));
	}

	@Test
	void inativaEReativa() throws Exception {
		when(service.inativar(5L)).thenReturn(Exemplos.sala(false));
		when(service.reativar(5L)).thenReturn(Exemplos.sala(true));

		mvc.perform(patch("/salas-cirurgicas/5/inativar")).andExpect(jsonPath("$.ativa").value(false));
		mvc.perform(patch("/salas-cirurgicas/5/reativar")).andExpect(jsonPath("$.ativa").value(true));
	}

	@Test
	void inativacaoComProcedimentoAtivoResponde422() throws Exception {
		when(service.inativar(5L)).thenThrow(new RegraNegocioException("A sala cirúrgica Sala 1 não pode ser "
				+ "inativada porque tem procedimento cirúrgico agendado ou em andamento."));

		mvc.perform(patch("/salas-cirurgicas/5/inativar"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.title").value("Regra de negócio violada"));
	}

}
