package br.pucminas.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

import br.pucminas.hospital.dto.ProfissionalSaudeAtualizacaoRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeRequest;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.service.ConsultaService;
import br.pucminas.hospital.service.ProcedimentoCirurgicoService;
import br.pucminas.hospital.service.ProfissionalSaudeService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfissionalSaudeController.class)
class ProfissionalSaudeControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ProfissionalSaudeService service;

	@MockitoBean
	private ConsultaService consultaService;

	@MockitoBean
	private ProcedimentoCirurgicoService procedimentoService;

	@Test
	void listaPorAreaOuPorSituacao() throws Exception {
		when(service.listarPorArea(AreaAtuacao.CARDIOLOGIA)).thenReturn(List.of(Exemplos.profissional()));
		when(service.listar(true)).thenReturn(List.of());

		mvc.perform(get("/profissionais").param("area", "CARDIOLOGIA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].categoria.descricao").value("Médico especialista"))
				.andExpect(jsonPath("$[0].areaAtuacao.descricao").value("Cardiologia"));
		mvc.perform(get("/profissionais").param("ativos", "true")).andExpect(status().isOk());
		verify(service).listar(true);
	}

	@Test
	void areaDesconhecidaResponde400() throws Exception {
		mvc.perform(get("/profissionais").param("area", "ASTROLOGIA"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O valor 'ASTROLOGIA' não é aceito para o parâmetro area."));
		verify(service, never()).listar(anyBoolean());
	}

	@Test
	void cadastraERespondeCreatedComLocation() throws Exception {
		when(service.cadastrar(any(ProfissionalSaudeRequest.class))).thenReturn(Exemplos.profissional());

		mvc.perform(post("/profissionais").contentType(MediaType.APPLICATION_JSON).content("""
				{"nome": "Dr. João Souza", "registroProfissional": "CRM-MG 2",
				 "categoria": "MEDICO_ESPECIALISTA", "areaAtuacao": "CARDIOLOGIA"}
				"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/profissionais/2"));
	}

	@Test
	void areaIncompativelComACategoriaResponde400() throws Exception {
		mvc.perform(post("/profissionais").contentType(MediaType.APPLICATION_JSON).content("""
				{"nome": "Dra. Ana", "registroProfissional": "CRM-MG 1", "categoria": "CLINICO_GERAL",
				 "areaAtuacao": "PEDIATRIA"}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("areaAtuacao"));
		verify(service, never()).cadastrar(any());
	}

	@Test
	void atualizaInativaEReativa() throws Exception {
		when(service.atualizar(eq(2L), any(ProfissionalSaudeAtualizacaoRequest.class))).thenReturn(Exemplos.profissional());
		when(service.inativar(2L)).thenReturn(Exemplos.profissional());
		when(service.reativar(2L)).thenReturn(Exemplos.profissional());

		mvc.perform(put("/profissionais/2").contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"Dr. João\"}"))
				.andExpect(status().isOk());
		mvc.perform(patch("/profissionais/2/inativar")).andExpect(status().isOk());
		mvc.perform(patch("/profissionais/2/reativar")).andExpect(status().isOk());
	}

	@Test
	void inativacaoImpedidaResponde422() throws Exception {
		when(service.inativar(2L)).thenThrow(new RegraNegocioException("O profissional Dr. João Souza não pode ser "
				+ "inativado porque tem sob sua responsabilidade: internação ativa."));

		mvc.perform(patch("/profissionais/2/inativar")).andExpect(status().isUnprocessableContent());
	}

	@Test
	void agendaDoProfissionalNaData() throws Exception {
		when(consultaService.agendaDoProfissional(2L, LocalDate.of(2030, 1, 15)))
				.thenReturn(List.of(Exemplos.consulta("AGENDADA")));

		mvc.perform(get("/profissionais/2/agenda").param("data", "2030-01-15"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].horario").value("14:00:00"));
	}

	@Test
	void agendaSemDataResponde400() throws Exception {
		mvc.perform(get("/profissionais/2/agenda"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O parâmetro data é obrigatório."));
	}

	@Test
	void procedimentosRealizados() throws Exception {
		when(procedimentoService.listarRealizadosPorCirurgiao(2L)).thenReturn(List.of(Exemplos.procedimento("CONCLUIDO")));

		mvc.perform(get("/profissionais/2/procedimentos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].descricao").value("Apendicectomia"));
	}

}
