package br.pucminas.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.dto.AtendimentoEmergenciaRequest;
import br.pucminas.hospital.dto.PrioridadeRequest;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.model.PrioridadeEmergencia;
import br.pucminas.hospital.model.SituacaoEmergencia;
import br.pucminas.hospital.service.AtendimentoEmergenciaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AtendimentoEmergenciaController.class)
class AtendimentoEmergenciaControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AtendimentoEmergenciaService service;

	@Test
	void registraChegadaERespondeCreatedComLocation() throws Exception {
		when(service.registrarChegada(any(AtendimentoEmergenciaRequest.class)))
				.thenReturn(Exemplos.emergencia("AGUARDANDO_AVALIACAO"));

		mvc.perform(post("/emergencias").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pacienteId\": 1, \"profissionalId\": 2}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/emergencias/30"));
	}

	@Test
	void chegadaSemPacienteResponde400() throws Exception {
		mvc.perform(post("/emergencias").contentType(MediaType.APPLICATION_JSON).content("{\"profissionalId\": 2}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("pacienteId"));
	}

	@Test
	void filaDeAvaliacao() throws Exception {
		when(service.filaDeAvaliacao()).thenReturn(List.of(Exemplos.emergencia("AGUARDANDO_AVALIACAO")));

		mvc.perform(get("/emergencias/fila"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].prioridade.codigo").value("ALTA"));
	}

	@Test
	void listaPorSituacao() throws Exception {
		when(service.listarPorSituacao(SituacaoEmergencia.EM_ATENDIMENTO))
				.thenReturn(List.of(Exemplos.emergencia("EM_ATENDIMENTO")));

		mvc.perform(get("/emergencias").param("situacao", "EM_ATENDIMENTO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void listagemSemSituacaoResponde400() throws Exception {
		mvc.perform(get("/emergencias"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O parâmetro situacao é obrigatório."));
	}

	@Test
	void buscaDefinePrioridadeIniciaEConclui() throws Exception {
		when(service.buscarPorId(30L)).thenReturn(Exemplos.emergencia("AGUARDANDO_AVALIACAO"));
		when(service.definirPrioridade(eq(30L), any(PrioridadeRequest.class)))
				.thenReturn(Exemplos.emergencia("AGUARDANDO_AVALIACAO"));
		when(service.iniciar(30L)).thenReturn(Exemplos.emergencia("EM_ATENDIMENTO"));
		when(service.concluir(30L)).thenReturn(Exemplos.emergencia("CONCLUIDO"));

		mvc.perform(get("/emergencias/30")).andExpect(status().isOk());
		mvc.perform(patch("/emergencias/30/definir-prioridade").contentType(MediaType.APPLICATION_JSON)
				.content("{\"prioridade\": \"ALTA\"}"))
				.andExpect(status().isOk());
		mvc.perform(patch("/emergencias/30/iniciar")).andExpect(jsonPath("$.situacao.codigo").value("EM_ATENDIMENTO"));
		mvc.perform(patch("/emergencias/30/concluir")).andExpect(jsonPath("$.situacao.codigo").value("CONCLUIDO"));
		org.mockito.Mockito.verify(service).definirPrioridade(30L, new PrioridadeRequest(PrioridadeEmergencia.ALTA));
	}

	@Test
	void prioridadeDesconhecidaResponde400() throws Exception {
		mvc.perform(patch("/emergencias/30/definir-prioridade").contentType(MediaType.APPLICATION_JSON)
				.content("{\"prioridade\": \"URGENTISSIMA\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void iniciarSemPrioridadeResponde422EConcluirSemIniciar409() throws Exception {
		when(service.iniciar(30L)).thenThrow(new RegraNegocioException(
				"Defina a prioridade do atendimento de emergência antes de iniciar o atendimento."));
		when(service.concluir(30L)).thenThrow(new TransicaoSituacaoInvalidaException(
				"Não é possível concluir um atendimento de emergência na situação Aguardando avaliação."));

		mvc.perform(patch("/emergencias/30/iniciar")).andExpect(status().isUnprocessableContent());
		mvc.perform(patch("/emergencias/30/concluir")).andExpect(status().isConflict());
	}

}
