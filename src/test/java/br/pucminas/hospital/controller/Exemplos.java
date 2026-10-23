package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.AtendimentoEmergenciaResponse;
import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.ConsultaResponse;
import br.pucminas.hospital.dto.InternacaoResponse;
import br.pucminas.hospital.dto.PacienteResponse;
import br.pucminas.hospital.dto.PessoaResumo;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoResponse;
import br.pucminas.hospital.dto.ProfissionalSaudeResponse;
import br.pucminas.hospital.dto.QuartoResponse;
import br.pucminas.hospital.dto.QuartoResumo;
import br.pucminas.hospital.model.SituacaoQuarto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Respostas de exemplo devolvidas pelos services simulados nos testes de controller.
 */
final class Exemplos {

	static final PessoaResumo MARIA = new PessoaResumo(1L, "Maria Silva");
	static final PessoaResumo DR_JOAO = new PessoaResumo(2L, "Dr. João Souza");

	private Exemplos() {
	}

	static PacienteResponse paciente(boolean ativo) {
		return new PacienteResponse(1L, "Maria Silva", "52998224725", LocalDate.of(1990, 5, 10), null,
				"maria@email.com", null, ativo);
	}

	static ProfissionalSaudeResponse profissional() {
		return new ProfissionalSaudeResponse(2L, "Dr. João Souza", "CRM-MG 2",
				new CodigoDescricao("MEDICO_ESPECIALISTA", "Médico especialista"),
				new CodigoDescricao("CARDIOLOGIA", "Cardiologia"), null, null, true);
	}

	static QuartoResponse quarto() {
		return new QuartoResponse(3L, "201", 2, new CodigoDescricao("UTI", "UTI"), 1, 0, SituacaoQuarto.DISPONIVEL);
	}

	static ConsultaResponse consulta(String situacao) {
		return new ConsultaResponse(10L, new CodigoDescricao("CONSULTA_ROTINA", "Consulta de rotina"), MARIA, DR_JOAO,
				LocalDate.of(2030, 1, 15), LocalTime.of(14, 0), "Check-up", null, new CodigoDescricao(situacao, situacao),
				null);
	}

	static InternacaoResponse internacao(boolean ativa) {
		return new InternacaoResponse(20L, MARIA, DR_JOAO, new QuartoResumo(3L, "201", new CodigoDescricao("UTI", "UTI")),
				LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 5), ativa ? null : LocalDate.of(2030, 2, 4), ativa, null);
	}

	static AtendimentoEmergenciaResponse emergencia(String situacao) {
		return new AtendimentoEmergenciaResponse(30L, MARIA, DR_JOAO, new CodigoDescricao("ALTA", "Alta"),
				LocalDateTime.of(2030, 1, 15, 22, 40), new CodigoDescricao(situacao, situacao), null);
	}

	static ProcedimentoCirurgicoResponse procedimento(String situacao) {
		return new ProcedimentoCirurgicoResponse(40L, MARIA, DR_JOAO, "Apendicectomia",
				new CodigoDescricao("URGENCIA", "Urgência"), LocalDateTime.of(2030, 3, 10, 8, 0), null, null,
				new CodigoDescricao(situacao, situacao), null, null, null);
	}

}
