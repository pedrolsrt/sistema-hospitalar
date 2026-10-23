package br.pucminas.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.dto.QuartoRequest;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.model.TipoQuarto;
import br.pucminas.hospital.service.QuartoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuartoController.class)
class QuartoControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private QuartoService service;

	@Test
	void listaComFiltrosOpcionais() throws Exception {
		when(service.listar(null, false)).thenReturn(List.of(Exemplos.quarto()));
		when(service.listar(TipoQuarto.UTI, true)).thenReturn(List.of());

		mvc.perform(get("/quartos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].tipo.codigo").value("UTI"))
				.andExpect(jsonPath("$[0].situacao").value("DISPONIVEL"));
		mvc.perform(get("/quartos").param("tipo", "UTI").param("comVaga", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void tipoDesconhecidoResponde400() throws Exception {
		mvc.perform(get("/quartos").param("tipo", "SUITE")).andExpect(status().isBadRequest());
	}

	@Test
	void cadastraERespondeCreatedComLocation() throws Exception {
		when(service.cadastrar(any(QuartoRequest.class))).thenReturn(Exemplos.quarto());

		mvc.perform(post("/quartos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"numero\": \"201\", \"andar\": 2, \"tipo\": \"UTI\", \"capacidadeMaxima\": 1}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/quartos/3"));
	}

	@Test
	void capacidadeAcimaDoLimiteDoTipoResponde422() throws Exception {
		when(service.cadastrar(any(QuartoRequest.class))).thenThrow(new RegraNegocioException(
				"Um quarto do tipo UTI comporta no máximo 1 leito(s); a capacidade informada foi 2."));

		mvc.perform(post("/quartos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"numero\": \"201\", \"andar\": 2, \"tipo\": \"UTI\", \"capacidadeMaxima\": 2}"))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void camposObrigatoriosResponde400() throws Exception {
		mvc.perform(post("/quartos").contentType(MediaType.APPLICATION_JSON).content("{\"capacidadeMaxima\": 0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.length()").value(4));
	}

	@Test
	void buscaEAtualiza() throws Exception {
		when(service.buscarPorId(3L)).thenReturn(Exemplos.quarto());
		when(service.atualizar(eq(3L), any(QuartoRequest.class))).thenReturn(Exemplos.quarto());

		mvc.perform(get("/quartos/3")).andExpect(status().isOk()).andExpect(jsonPath("$.numero").value("201"));
		mvc.perform(put("/quartos/3").contentType(MediaType.APPLICATION_JSON)
				.content("{\"numero\": \"201\", \"andar\": 2, \"tipo\": \"UTI\", \"capacidadeMaxima\": 1}"))
				.andExpect(status().isOk());
	}

	@Test
	void removeRespondendo204() throws Exception {
		mvc.perform(delete("/quartos/3")).andExpect(status().isNoContent()).andExpect(content().string(""));
		verify(service).remover(3L);
	}

	@Test
	void remocaoImpedidaResponde422() throws Exception {
		doThrow(new RegraNegocioException("O quarto 201 tem internação ativa e não pode ser removido."))
				.when(service).remover(3L);

		mvc.perform(delete("/quartos/3"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("O quarto 201 tem internação ativa e não pode ser removido."));
	}

}
