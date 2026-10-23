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

import br.pucminas.hospital.dto.ConsultaEspecializadaRequest;
import br.pucminas.hospital.dto.ConsultaRotinaRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.model.SituacaoConsulta;
import br.pucminas.hospital.service.ConsultaService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConsultaController.class)
class ConsultaControllerTest {

	private static final String ROTINA = """
			{"pacienteId": 1, "profissionalId": 2, "data": "2030-01-15", "horario": "14:00", "motivo": "Check-up"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultaService service;

	@Test
	void agendaConsultaDeRotina() throws Exception {
		when(service.agendarRotina(any(ConsultaRotinaRequest.class))).thenReturn(Exemplos.consulta("AGENDADA"));

		mvc.perform(post("/consultas/rotina").contentType(MediaType.APPLICATION_JSON).content(ROTINA))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/consultas/10"))
				.andExpect(jsonPath("$.situacao.codigo").value("AGENDADA"));
	}

	@Test
	void agendaConsultaEspecializadaComLocationDaConsulta() throws Exception {
		when(service.agendarEspecializada(any(ConsultaEspecializadaRequest.class)))
				.thenReturn(Exemplos.consulta("AGENDADA"));

		mvc.perform(post("/consultas/especializadas").contentType(MediaType.APPLICATION_JSON).content("""
				{"pacienteId": 1, "profissionalId": 2, "data": "2030-01-15", "horario": "14:00", "motivo": "Dor",
				 "areaAtuacao": "CARDIOLOGIA"}
				"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/consultas/10"));
	}

	@Test
	void consultaEspecializadaSemAreaResponde400() throws Exception {
		mvc.perform(post("/consultas/especializadas").contentType(MediaType.APPLICATION_JSON).content(ROTINA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("areaAtuacao"));
		verify(service, never()).agendarEspecializada(any());
	}

	@Test
	void horarioOcupadoResponde422() throws Exception {
		when(service.agendarRotina(any(ConsultaRotinaRequest.class))).thenThrow(new RegraNegocioException(
				"O profissional Dr. João Souza já tem consulta em 15/01/2030 às 14:00, que se sobrepõe ao horário pedido "
						+ "(cada consulta dura 30 minutos)."));

		mvc.perform(post("/consultas/rotina").contentType(MediaType.APPLICATION_JSON).content(ROTINA))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.title").value("Regra de negócio violada"));
	}

	@Test
	void listaComFiltros() throws Exception {
		when(service.listar(SituacaoConsulta.AGENDADA, 2L, LocalDate.of(2030, 1, 15)))
				.thenReturn(List.of(Exemplos.consulta("AGENDADA")));
		when(service.listar(null, null, null)).thenReturn(List.of());

		mvc.perform(get("/consultas").param("situacao", "AGENDADA").param("profissionalId", "2")
				.param("data", "2030-01-15"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mvc.perform(get("/consultas")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void dataMalFormadaNoFiltroResponde400() throws Exception {
		mvc.perform(get("/consultas").param("data", "15/01/2030")).andExpect(status().isBadRequest());
	}

	@Test
	void cadaAcaoChamaOMetodoDoService() throws Exception {
		when(service.buscarPorId(10L)).thenReturn(Exemplos.consulta("AGENDADA"));
		when(service.confirmar(10L)).thenReturn(Exemplos.consulta("CONFIRMADA"));
		when(service.iniciarAtendimento(10L)).thenReturn(Exemplos.consulta("EM_ATENDIMENTO"));
		when(service.concluir(10L)).thenReturn(Exemplos.consulta("CONCLUIDA"));
		when(service.cancelar(10L)).thenReturn(Exemplos.consulta("CANCELADA"));
		when(service.registrarNaoComparecimento(10L)).thenReturn(Exemplos.consulta("NAO_COMPARECEU"));

		mvc.perform(get("/consultas/10")).andExpect(jsonPath("$.situacao.codigo").value("AGENDADA"));
		mvc.perform(patch("/consultas/10/confirmar")).andExpect(jsonPath("$.situacao.codigo").value("CONFIRMADA"));
		mvc.perform(patch("/consultas/10/iniciar")).andExpect(jsonPath("$.situacao.codigo").value("EM_ATENDIMENTO"));
		mvc.perform(patch("/consultas/10/concluir")).andExpect(jsonPath("$.situacao.codigo").value("CONCLUIDA"));
		mvc.perform(patch("/consultas/10/cancelar")).andExpect(jsonPath("$.situacao.codigo").value("CANCELADA"));
		mvc.perform(patch("/consultas/10/registrar-nao-comparecimento"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.situacao.codigo").value("NAO_COMPARECEU"));
	}

	@Test
	void transicaoInvalidaResponde409() throws Exception {
		when(service.concluir(10L)).thenThrow(
				new TransicaoSituacaoInvalidaException("Não é possível concluir uma consulta na situação Agendada."));

		mvc.perform(patch("/consultas/10/concluir"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Transição de situação inválida"))
				.andExpect(jsonPath("$.detail").value("Não é possível concluir uma consulta na situação Agendada."));
	}

	@Test
	void registraObservacoes() throws Exception {
		when(service.registrarObservacoes(eq(10L), any(ObservacoesRequest.class)))
				.thenReturn(Exemplos.consulta("EM_ATENDIMENTO"));

		mvc.perform(patch("/consultas/10/registrar-observacoes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\": \"Pressão 12x8.\"}"))
				.andExpect(status().isOk());
		verify(service).registrarObservacoes(10L, new ObservacoesRequest("Pressão 12x8."));
	}

	@Test
	void observacoesEmBrancoResponde400() throws Exception {
		mvc.perform(patch("/consultas/10/registrar-observacoes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\": \" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].mensagem").value("O texto das observações é obrigatório."));
	}

	@Test
	void metodoErradoResponde405() throws Exception {
		mvc.perform(post("/consultas/10/confirmar")).andExpect(status().isMethodNotAllowed());
	}

}
