package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.ConsultaEspecializadaRequest;
import br.pucminas.hospital.dto.ConsultaResponse;
import br.pucminas.hospital.dto.ConsultaRotinaRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.model.SituacaoConsulta;
import br.pucminas.hospital.service.ConsultaService;
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
@RequestMapping("/consultas")
@Tag(name = "Consultas", description = "Consultas de rotina e especializadas. Situações: AGENDADA -> CONFIRMADA -> "
		+ "EM_ATENDIMENTO -> CONCLUIDA; agendada ou confirmada pode ser cancelada ou ter o não comparecimento "
		+ "registrado. Remarcar significa agendar uma nova consulta.")
public class ConsultaController {

	private final ConsultaService service;

	public ConsultaController(ConsultaService service) {
		this.service = service;
	}

	@PostMapping("/rotina")
	@Operation(summary = "Agenda uma consulta de rotina", description = "Exige paciente e profissional ativos, data e "
			+ "horário futuros e nenhuma sobreposição com consultas ativas do profissional ou do paciente.")
	@ApiResponse(responseCode = "201", description = "Consulta agendada; o cabeçalho Location aponta para ela")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Paciente ou profissional não encontrado")
	@ApiResponse(responseCode = "422", description = "Data no passado, participante inativo ou horário ocupado")
	public ResponseEntity<ConsultaResponse> agendarRotina(@Valid @RequestBody ConsultaRotinaRequest request) {
		ConsultaResponse resposta = service.agendarRotina(request);
		return ResponseEntity.created(Localizacao.de("/consultas/{id}", resposta.id())).body(resposta);
	}

	@PostMapping("/especializadas")
	@Operation(summary = "Agenda uma consulta especializada", description = "Além das regras da consulta de rotina, "
			+ "exige um médico especialista ativo da área da consulta.")
	@ApiResponse(responseCode = "201", description = "Consulta agendada; o cabeçalho Location aponta para ela")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Paciente ou profissional não encontrado")
	@ApiResponse(responseCode = "422", description = "Profissional não é especialista da área, data no passado, "
			+ "participante inativo ou horário ocupado")
	public ResponseEntity<ConsultaResponse> agendarEspecializada(
			@Valid @RequestBody ConsultaEspecializadaRequest request) {
		ConsultaResponse resposta = service.agendarEspecializada(request);
		return ResponseEntity.created(Localizacao.de("/consultas/{id}", resposta.id())).body(resposta);
	}

	@GetMapping
	@Operation(summary = "Lista as consultas", description = "Em ordem de data e horário; os filtros são opcionais e "
			+ "podem ser combinados.")
	@ApiResponse(responseCode = "200", description = "Consultas encontradas")
	@ApiResponse(responseCode = "400", description = "Filtro com valor inválido")
	public List<ConsultaResponse> listar(
			@Parameter(description = "Situação da consulta") @RequestParam(required = false) SituacaoConsulta situacao,
			@Parameter(description = "Id do profissional responsável") @RequestParam(required = false) Long profissionalId,
			@Parameter(description = "Data no formato AAAA-MM-DD", example = "2030-01-15")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
		return service.listar(situacao, profissionalId, data);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma consulta pelo id")
	@ApiResponse(responseCode = "200", description = "Consulta encontrada")
	@ApiResponse(responseCode = "404", description = "Consulta não encontrada")
	public ConsultaResponse buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PatchMapping("/{id}/confirmar")
	@Operation(summary = "Confirma uma consulta agendada")
	@ApiResponse(responseCode = "200", description = "Consulta confirmada")
	@ApiResponse(responseCode = "404", description = "Consulta não encontrada")
	@ApiResponse(responseCode = "409", description = "A consulta não está agendada")
	public ConsultaResponse confirmar(@PathVariable Long id) {
		return service.confirmar(id);
	}

	@PatchMapping("/{id}/iniciar")
	@Operation(summary = "Inicia o atendimento de uma consulta confirmada",
			description = "Verifica de novo se a consulta pode ser realizada (participantes ativos, especialista da área).")
	@ApiResponse(responseCode = "200", description = "Consulta em atendimento")
	@ApiResponse(responseCode = "404", description = "Consulta não encontrada")
	@ApiResponse(responseCode = "409", description = "A consulta não está confirmada")
	@ApiResponse(responseCode = "422", description = "A consulta não pode mais ser realizada")
	public ConsultaResponse iniciar(@PathVariable Long id) {
		return service.iniciarAtendimento(id);
	}

	@PatchMapping("/{id}/concluir")
	@Operation(summary = "Conclui uma consulta em atendimento")
	@ApiResponse(responseCode = "200", description = "Consulta concluída")
	@ApiResponse(responseCode = "404", description = "Consulta não encontrada")
	@ApiResponse(responseCode = "409", description = "A consulta não está em atendimento")
	public ConsultaResponse concluir(@PathVariable Long id) {
		return service.concluir(id);
	}

	@PatchMapping("/{id}/cancelar")
	@Operation(summary = "Cancela uma consulta agendada ou confirmada")
	@ApiResponse(responseCode = "200", description = "Consulta cancelada")
	@ApiResponse(responseCode = "404", description = "Consulta não encontrada")
	@ApiResponse(responseCode = "409", description = "A consulta não está agendada nem confirmada")
	public ConsultaResponse cancelar(@PathVariable Long id) {
		return service.cancelar(id);
	}

	@PatchMapping("/{id}/registrar-nao-comparecimento")
	@Operation(summary = "Registra que o paciente não compareceu a uma consulta agendada ou confirmada")
	@ApiResponse(responseCode = "200", description = "Não comparecimento registrado")
	@ApiResponse(responseCode = "404", description = "Consulta não encontrada")
	@ApiResponse(responseCode = "409", description = "A consulta não está agendada nem confirmada")
	public ConsultaResponse registrarNaoComparecimento(@PathVariable Long id) {
		return service.registrarNaoComparecimento(id);
	}

	@PatchMapping("/{id}/registrar-observacoes")
	@Operation(summary = "Registra as observações da consulta",
			description = "Permitido apenas durante o atendimento ou depois da conclusão.")
	@ApiResponse(responseCode = "200", description = "Observações registradas")
	@ApiResponse(responseCode = "400", description = "Texto ausente ou longo demais")
	@ApiResponse(responseCode = "404", description = "Consulta não encontrada")
	@ApiResponse(responseCode = "422", description = "A consulta não está em atendimento nem concluída")
	public ConsultaResponse registrarObservacoes(@PathVariable Long id, @Valid @RequestBody ObservacoesRequest request) {
		return service.registrarObservacoes(id, request);
	}

}
