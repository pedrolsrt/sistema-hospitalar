package br.pucminas.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucminas.hospital.dto.AtendimentoItemResponse;
import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.model.FiltroAtendimentos;
import br.pucminas.hospital.model.ModalidadeAtendimento;
import br.pucminas.hospital.model.OrdenacaoAtendimentos;
import br.pucminas.hospital.service.AtendimentoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AtendimentoController.class)
class AtendimentoControllerTest {

	private static final FiltroAtendimentos SEM_FILTRO = new FiltroAtendimentos(null, null, null, null, null, null);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AtendimentoService service;

	private static AtendimentoItemResponse consulta() {
		return new AtendimentoItemResponse(LocalDateTime.of(2030, 1, 15, 14, 0),
				new CodigoDescricao("CONSULTA_ROTINA", "Consulta de rotina"),
				new CodigoDescricao("AGENDADA", "Agendada"), Exemplos.consulta("AGENDADA"));
	}

	@Test
	void semParametrosBuscaTudoEmOrdemDeData() throws Exception {
		when(service.listar(SEM_FILTRO, OrdenacaoAtendimentos.DATA)).thenReturn(List.of(consulta()));

		mvc.perform(get("/atendimentos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].modalidade.codigo").value("CONSULTA_ROTINA"))
				.andExpect(jsonPath("$[0].situacao.codigo").value("AGENDADA"))
				.andExpect(jsonPath("$[0].detalhes.motivo").value("Check-up"));
	}

	@Test
	void repassaTodosOsFiltrosEAOrdenacao() throws Exception {
		FiltroAtendimentos filtro = new FiltroAtendimentos(1L, 2L, "AGENDADA", ModalidadeAtendimento.CONSULTA_ROTINA,
				LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31));
		when(service.listar(filtro, OrdenacaoAtendimentos.PACIENTE)).thenReturn(List.of(consulta()));

		mvc.perform(get("/atendimentos").param("pacienteId", "1").param("profissionalId", "2")
				.param("situacao", "AGENDADA").param("modalidade", "CONSULTA_ROTINA")
				.param("inicio", "2030-01-01").param("fim", "2030-01-31").param("ordem", "PACIENTE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void ordenacaoOuModalidadeDesconhecidaResponde400() throws Exception {
		mvc.perform(get("/atendimentos").param("ordem", "ALFABETICA")).andExpect(status().isBadRequest());
		mvc.perform(get("/atendimentos").param("modalidade", "CIRURGIA")).andExpect(status().isBadRequest());
		verify(service, never()).listar(any(), any());
	}

	@Test
	void situacaoNaoReconhecidaResponde400ListandoOsCodigosValidos() throws Exception {
		mvc.perform(get("/atendimentos").param("situacao", "FINALIZADA"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.campos[0].campo").value("situacao"))
				.andExpect(jsonPath("$.campos[0].mensagem").value("A situação informada não foi reconhecida. Use um "
						+ "destes códigos: AGENDADA, CONFIRMADA, EM_ATENDIMENTO, CONCLUIDA, CANCELADA, NAO_COMPARECEU, "
						+ "AGUARDANDO_AVALIACAO, CONCLUIDO, AGENDADO, EM_ANDAMENTO, CANCELADO, ATIVA, ENCERRADA."));
		verify(service, never()).listar(any(), any());
	}

	@Test
	void situacaoDeQualquerModalidadeEAceita() throws Exception {
		for (String situacao : List.of("CONCLUIDA", "CONCLUIDO", "AGUARDANDO_AVALIACAO", "ENCERRADA")) {
			mvc.perform(get("/atendimentos").param("situacao", situacao)).andExpect(status().isOk());
		}
	}

	@Test
	void inicioPosteriorAoFimResponde422() throws Exception {
		mvc.perform(get("/atendimentos").param("inicio", "2030-02-01").param("fim", "2030-01-31"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail")
						.value("O início do período (01/02/2030) não pode ser posterior ao fim (31/01/2030)."));
		verify(service, never()).listar(any(), any());
	}

}
