package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.AtendimentoItemResponse;
import br.pucminas.hospital.dto.SituacaoReconhecida;
import br.pucminas.hospital.model.FiltroAtendimentos;
import br.pucminas.hospital.model.ModalidadeAtendimento;
import br.pucminas.hospital.model.OrdenacaoAtendimentos;
import br.pucminas.hospital.service.AtendimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/atendimentos")
@Tag(name = "Atendimentos", description = "Busca de atendimentos de todas as modalidades (consultas, internações, "
		+ "emergências e procedimentos), com filtros combináveis e ordenação à escolha.")
public class AtendimentoController {

	private final AtendimentoService service;

	public AtendimentoController(AtendimentoService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Busca atendimentos", description = "Todos os filtros são opcionais; os informados são "
			+ "combinados (todos precisam ser atendidos). O período compara a data de referência de cada "
			+ "atendimento: data e horário da consulta, entrada da internação, chegada da emergência e início "
			+ "(ou previsão) do procedimento.")
	@ApiResponse(responseCode = "200", description = "Atendimentos encontrados, com a modalidade, a situação e os "
			+ "dados próprios de cada um")
	@ApiResponse(responseCode = "400", description = "Filtro ou ordenação com valor inválido, inclusive situação não "
			+ "reconhecida; nesse caso a mensagem lista os códigos válidos")
	@ApiResponse(responseCode = "422", description = "Início do período posterior ao fim")
	public List<AtendimentoItemResponse> buscar(
			@Parameter(description = "Id do paciente") @RequestParam(required = false) Long pacienteId,
			@Parameter(description = "Id do profissional responsável")
			@RequestParam(required = false) Long profissionalId,
			@Parameter(description = "Código da situação na modalidade do atendimento, por exemplo AGENDADA, "
					+ "CONCLUIDA (consulta), CONCLUIDO (emergência e procedimento), ATIVA ou ENCERRADA (internação); "
					+ "um código que não existe em nenhuma modalidade responde 400")
			@RequestParam(required = false) @SituacaoReconhecida String situacao,
			@Parameter(description = "Modalidade do atendimento") @RequestParam(required = false)
			ModalidadeAtendimento modalidade,
			@Parameter(description = "Primeira data do período, no formato AAAA-MM-DD", example = "2030-01-01")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@Parameter(description = "Última data do período, incluída, no formato AAAA-MM-DD", example = "2030-01-31")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
			@Parameter(description = "Ordenação: DATA (padrão), DATA_DECRESCENTE, PACIENTE ou PRIORIDADE")
			@RequestParam(defaultValue = "DATA") OrdenacaoAtendimentos ordem) {
		return service.listar(new FiltroAtendimentos(pacienteId, profissionalId, situacao, modalidade, inicio, fim),
				ordem);
	}

}
