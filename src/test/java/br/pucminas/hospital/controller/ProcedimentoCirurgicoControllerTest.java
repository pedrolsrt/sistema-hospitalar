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

import br.pucminas.hospital.dto.ConclusaoProcedimentoRequest;
import br.pucminas.hospital.dto.InicioProcedimentoRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoRequest;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.service.ProcedimentoCirurgicoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProcedimentoCirurgicoController.class)
class ProcedimentoCirurgicoControllerTest {

	private static final String PROCEDIMENTO = """
			{"pacienteId": 1, "cirurgiaoId": 2, "descricao": "Apendicectomia", "tipoCirurgia": "URGENCIA",
			 "dataHoraPrevista": "2030-03-10T08:00:00"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ProcedimentoCirurgicoService service;

	@Test
	void agendaERespondeCreatedComLocation() throws Exception {
		when(service.agendar(any(ProcedimentoCirurgicoRequest.class))).thenReturn(Exemplos.procedimento("AGENDADO"));

		mvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON).content(PROCEDIMENTO))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/procedimentos/40"))
				.andExpect(jsonPath("$.tipoCirurgia.descricao").value("Urgência"));
	}

	@Test
	void profissionalQueNaoECirurgiaoResponde422() throws Exception {
		when(service.agendar(any(ProcedimentoCirurgicoRequest.class))).thenThrow(new RegraNegocioException(
				"O profissional Dra. Ana Lima não pode realizar procedimentos cirúrgicos: é necessário um cirurgião."));

		mvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON).content(PROCEDIMENTO))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void tipoDeCirurgiaDesconhecidoResponde400() throws Exception {
		mvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
				.content(PROCEDIMENTO.replace("URGENCIA", "ESTETICA")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Requisição inválida"));
	}

	@Test
	void listaPorData() throws Exception {
		when(service.listarPorData(LocalDate.of(2030, 3, 10))).thenReturn(List.of(Exemplos.procedimento("AGENDADO")));

		mvc.perform(get("/procedimentos").param("data", "2030-03-10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].dataHoraPrevista").value("2030-03-10T08:00:00"));
	}

	@Test
	void buscaIniciaConcluiECancela() throws Exception {
		LocalDateTime inicio = LocalDateTime.of(2030, 3, 10, 8, 15);
		when(service.buscarPorId(40L)).thenReturn(Exemplos.procedimento("AGENDADO"));
		when(service.iniciar(40L, new InicioProcedimentoRequest(inicio))).thenReturn(Exemplos.procedimento("EM_ANDAMENTO"));
		when(service.concluir(40L, new ConclusaoProcedimentoRequest(inicio.plusHours(1))))
				.thenReturn(Exemplos.procedimento("CONCLUIDO"));
		when(service.cancelar(40L)).thenReturn(Exemplos.procedimento("CANCELADO"));

		mvc.perform(get("/procedimentos/40")).andExpect(status().isOk());
		mvc.perform(patch("/procedimentos/40/iniciar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"dataHoraInicio\": \"2030-03-10T08:15:00\"}"))
				.andExpect(jsonPath("$.situacao.codigo").value("EM_ANDAMENTO"));
		mvc.perform(patch("/procedimentos/40/concluir").contentType(MediaType.APPLICATION_JSON)
				.content("{\"dataHoraFim\": \"2030-03-10T09:15:00\"}"))
				.andExpect(jsonPath("$.situacao.codigo").value("CONCLUIDO"));
		mvc.perform(patch("/procedimentos/40/cancelar")).andExpect(jsonPath("$.situacao.codigo").value("CANCELADO"));
	}

	@Test
	void iniciarSemDataResponde400() throws Exception {
		mvc.perform(patch("/procedimentos/40/iniciar").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("dataHoraInicio"));
		verify(service, never()).iniciar(any(), any());
	}

	@Test
	void concluirSemIniciarResponde409() throws Exception {
		when(service.concluir(eq(40L), any(ConclusaoProcedimentoRequest.class))).thenThrow(
				new TransicaoSituacaoInvalidaException("Não é possível concluir um procedimento cirúrgico na situação Agendado."));

		mvc.perform(patch("/procedimentos/40/concluir").contentType(MediaType.APPLICATION_JSON)
				.content("{\"dataHoraFim\": \"2030-03-10T09:15:00\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Não é possível concluir um procedimento cirúrgico na situação Agendado."));
	}

	@Test
	void registraObservacoesPreEPosOperatorias() throws Exception {
		when(service.registrarObservacoesPreOperatorias(eq(40L), any(ObservacoesRequest.class)))
				.thenReturn(Exemplos.procedimento("AGENDADO"));
		when(service.registrarObservacoesPosOperatorias(eq(40L), any(ObservacoesRequest.class)))
				.thenReturn(Exemplos.procedimento("EM_ANDAMENTO"));

		mvc.perform(patch("/procedimentos/40/registrar-observacoes-pre-operatorias").contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\": \"Jejum de 8 horas.\"}"))
				.andExpect(status().isOk());
		mvc.perform(patch("/procedimentos/40/registrar-observacoes-pos-operatorias").contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\": \"Sem intercorrências.\"}"))
				.andExpect(status().isOk());
		verify(service).registrarObservacoesPreOperatorias(40L, new ObservacoesRequest("Jejum de 8 horas."));
		verify(service).registrarObservacoesPosOperatorias(40L, new ObservacoesRequest("Sem intercorrências."));
	}

}
