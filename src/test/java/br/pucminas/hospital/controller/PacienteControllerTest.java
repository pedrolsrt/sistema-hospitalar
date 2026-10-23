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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.HistoricoItemResponse;
import br.pucminas.hospital.dto.HistoricoMedicoResponse;
import br.pucminas.hospital.dto.PacienteAtualizacaoRequest;
import br.pucminas.hospital.dto.PacienteRequest;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.service.HistoricoMedicoService;
import br.pucminas.hospital.service.PacienteService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PacienteController.class)
class PacienteControllerTest {

	private static final String PACIENTE_VALIDO = """
			{"nome": "Maria Silva", "cpf": "529.982.247-25", "dataNascimento": "1990-05-10", "email": "maria@email.com"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private PacienteService service;

	@MockitoBean
	private HistoricoMedicoService historicoService;

	@Test
	void listaTodosPorPadraoOuSoOsAtivos() throws Exception {
		when(service.listar(false)).thenReturn(List.of(Exemplos.paciente(true), Exemplos.paciente(false)));
		when(service.listar(true)).thenReturn(List.of(Exemplos.paciente(true)));

		mvc.perform(get("/pacientes")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
		mvc.perform(get("/pacientes").param("ativos", "true")).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void buscaPorId() throws Exception {
		when(service.buscarPorId(1L)).thenReturn(Exemplos.paciente(true));

		mvc.perform(get("/pacientes/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Maria Silva"))
				.andExpect(jsonPath("$.dataNascimento").value("1990-05-10"));
	}

	@Test
	void pacienteInexistenteResponde404NoFormatoProblemDetail() throws Exception {
		when(service.buscarPorId(9L)).thenThrow(new RecursoNaoEncontradoException("Paciente não encontrado (id 9)."));

		mvc.perform(get("/pacientes/9"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Recurso não encontrado"))
				.andExpect(jsonPath("$.detail").value("Paciente não encontrado (id 9)."))
				.andExpect(jsonPath("$.instance").value("/pacientes/9"));
	}

	@Test
	void cadastraERespondeCreatedComLocation() throws Exception {
		when(service.cadastrar(any(PacienteRequest.class))).thenReturn(Exemplos.paciente(true));

		mvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON).content(PACIENTE_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/pacientes/1"))
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void naoCadastraDadosInvalidos() throws Exception {
		mvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"\", \"cpf\": \"123\", \"dataNascimento\": \"2999-01-01\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[*].campo").value(org.hamcrest.Matchers.containsInAnyOrder(
						"cpf", "dataNascimento", "nome")));
		verify(service, never()).cadastrar(any());
	}

	@Test
	void cpfDuplicadoResponde409() throws Exception {
		when(service.cadastrar(any(PacienteRequest.class)))
				.thenThrow(new DadoJaCadastradoException("Já existe um paciente cadastrado com o CPF 529.982.247-25."));

		mvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON).content(PACIENTE_VALIDO))
				.andExpect(status().isConflict());
	}

	@Test
	void atualiza() throws Exception {
		when(service.atualizar(eq(1L), any(PacienteAtualizacaoRequest.class))).thenReturn(Exemplos.paciente(true));

		mvc.perform(put("/pacientes/1").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"Maria Souza\", \"dataNascimento\": \"1990-05-10\"}"))
				.andExpect(status().isOk());
		verify(service).atualizar(eq(1L), any(PacienteAtualizacaoRequest.class));
	}

	@Test
	void inativaEReativa() throws Exception {
		when(service.inativar(1L)).thenReturn(Exemplos.paciente(false));
		when(service.reativar(1L)).thenReturn(Exemplos.paciente(true));

		mvc.perform(patch("/pacientes/1/inativar")).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
		mvc.perform(patch("/pacientes/1/reativar")).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true));
	}

	@Test
	void inativacaoImpedidaResponde422() throws Exception {
		when(service.inativar(1L)).thenThrow(new RegraNegocioException(
				"O paciente Maria Silva não pode ser inativado porque tem: internação ativa."));

		mvc.perform(patch("/pacientes/1/inativar"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("O paciente Maria Silva não pode ser inativado porque tem: "
						+ "internação ativa."));
	}

	@Test
	void historicoMedico() throws Exception {
		when(historicoService.montar(1L)).thenReturn(new HistoricoMedicoResponse(Exemplos.MARIA, List.of(
				new HistoricoItemResponse(LocalDateTime.of(2030, 1, 15, 14, 0),
						new CodigoDescricao("CONSULTA_ROTINA", "Consulta de rotina"), Exemplos.consulta("CONCLUIDA")))));

		mvc.perform(get("/pacientes/1/historico"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paciente.nome").value("Maria Silva"))
				.andExpect(jsonPath("$.atendimentos[0].modalidade.codigo").value("CONSULTA_ROTINA"))
				.andExpect(jsonPath("$.atendimentos[0].detalhes.motivo").value("Check-up"));
	}

}
