package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.ConclusaoProcedimentoRequest;
import br.pucminas.hospital.dto.InicioProcedimentoRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoRequest;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoResponse;
import br.pucminas.hospital.service.ProcedimentoCirurgicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/procedimentos")
@Tag(name = "Procedimentos cirúrgicos", description = "Procedimentos realizados por cirurgiões. Situações: AGENDADO -> "
		+ "EM_ANDAMENTO -> CONCLUIDO; só o agendado pode ser cancelado, e não se conclui sem ter iniciado.")
public class ProcedimentoCirurgicoController {

	private final ProcedimentoCirurgicoService service;

	public ProcedimentoCirurgicoController(ProcedimentoCirurgicoService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Agenda um procedimento", description = "Exige um cirurgião ativo, paciente ativo, "
			+ "descrição, tipo de cirurgia e data e hora prevista.")
	@ApiResponse(responseCode = "201", description = "Procedimento agendado; o cabeçalho Location aponta para ele")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Paciente ou profissional não encontrado")
	@ApiResponse(responseCode = "422", description = "O profissional não é cirurgião ou há participante inativo")
	public ResponseEntity<ProcedimentoCirurgicoResponse> agendar(
			@Valid @RequestBody ProcedimentoCirurgicoRequest request) {
		ProcedimentoCirurgicoResponse resposta = service.agendar(request);
		return ResponseEntity.created(Localizacao.de("/procedimentos/{id}", resposta.id())).body(resposta);
	}

	@GetMapping
	@Operation(summary = "Lista os procedimentos previstos para uma data",
			description = "Em qualquer situação, em ordem de horário previsto.")
	@ApiResponse(responseCode = "200", description = "Procedimentos encontrados")
	@ApiResponse(responseCode = "400", description = "Data ausente ou inválida")
	public List<ProcedimentoCirurgicoResponse> listar(
			@Parameter(description = "Data no formato AAAA-MM-DD", example = "2030-03-10")
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
		return service.listarPorData(data);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um procedimento pelo id")
	@ApiResponse(responseCode = "200", description = "Procedimento encontrado")
	@ApiResponse(responseCode = "404", description = "Procedimento não encontrado")
	public ProcedimentoCirurgicoResponse buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PatchMapping("/{id}/iniciar")
	@Operation(summary = "Inicia um procedimento agendado",
			description = "Verifica de novo se o procedimento pode ser realizado (cirurgião e paciente ativos).")
	@ApiResponse(responseCode = "200", description = "Procedimento em andamento")
	@ApiResponse(responseCode = "400", description = "Data e hora de início ausente")
	@ApiResponse(responseCode = "404", description = "Procedimento não encontrado")
	@ApiResponse(responseCode = "409", description = "O procedimento não está agendado")
	@ApiResponse(responseCode = "422", description = "O procedimento não pode mais ser realizado")
	public ProcedimentoCirurgicoResponse iniciar(@PathVariable Long id,
			@Valid @RequestBody InicioProcedimentoRequest request) {
		return service.iniciar(id, request);
	}

	@PatchMapping("/{id}/concluir")
	@Operation(summary = "Conclui um procedimento em andamento", description = "O fim não pode ser anterior ao início.")
	@ApiResponse(responseCode = "200", description = "Procedimento concluído")
	@ApiResponse(responseCode = "400", description = "Data e hora de fim ausente")
	@ApiResponse(responseCode = "404", description = "Procedimento não encontrado")
	@ApiResponse(responseCode = "409", description = "O procedimento não está em andamento (por exemplo, não foi iniciado)")
	@ApiResponse(responseCode = "422", description = "Fim anterior ao início")
	public ProcedimentoCirurgicoResponse concluir(@PathVariable Long id,
			@Valid @RequestBody ConclusaoProcedimentoRequest request) {
		return service.concluir(id, request);
	}

	@PatchMapping("/{id}/cancelar")
	@Operation(summary = "Cancela um procedimento agendado")
	@ApiResponse(responseCode = "200", description = "Procedimento cancelado")
	@ApiResponse(responseCode = "404", description = "Procedimento não encontrado")
	@ApiResponse(responseCode = "409", description = "O procedimento não está agendado")
	public ProcedimentoCirurgicoResponse cancelar(@PathVariable Long id) {
		return service.cancelar(id);
	}

	@PatchMapping("/{id}/registrar-observacoes-pre-operatorias")
	@Operation(summary = "Registra as observações pré-operatórias")
	@ApiResponse(responseCode = "200", description = "Observações registradas")
	@ApiResponse(responseCode = "400", description = "Texto ausente ou longo demais")
	@ApiResponse(responseCode = "404", description = "Procedimento não encontrado")
	public ProcedimentoCirurgicoResponse registrarObservacoesPreOperatorias(@PathVariable Long id,
			@Valid @RequestBody ObservacoesRequest request) {
		return service.registrarObservacoesPreOperatorias(id, request);
	}

	@PatchMapping("/{id}/registrar-observacoes-pos-operatorias")
	@Operation(summary = "Registra as observações pós-operatórias",
			description = "Permitido depois que o procedimento é iniciado.")
	@ApiResponse(responseCode = "200", description = "Observações registradas")
	@ApiResponse(responseCode = "400", description = "Texto ausente ou longo demais")
	@ApiResponse(responseCode = "404", description = "Procedimento não encontrado")
	@ApiResponse(responseCode = "422", description = "O procedimento ainda não foi iniciado")
	public ProcedimentoCirurgicoResponse registrarObservacoesPosOperatorias(@PathVariable Long id,
			@Valid @RequestBody ObservacoesRequest request) {
		return service.registrarObservacoesPosOperatorias(id, request);
	}

}
