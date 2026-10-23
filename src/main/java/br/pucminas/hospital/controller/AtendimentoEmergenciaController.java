package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.AtendimentoEmergenciaRequest;
import br.pucminas.hospital.dto.AtendimentoEmergenciaResponse;
import br.pucminas.hospital.dto.PrioridadeRequest;
import br.pucminas.hospital.model.SituacaoEmergencia;
import br.pucminas.hospital.service.AtendimentoEmergenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/emergencias")
@Tag(name = "Emergências", description = "Atendimentos de emergência. Situações: AGUARDANDO_AVALIACAO -> "
		+ "EM_ATENDIMENTO -> CONCLUIDO. A prioridade pode ser definida na chegada ou na avaliação e é exigida para iniciar.")
public class AtendimentoEmergenciaController {

	private final AtendimentoEmergenciaService service;

	public AtendimentoEmergenciaController(AtendimentoEmergenciaService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Registra a chegada de um paciente", description = "Com o profissional de plantão como "
			+ "responsável. Sem data e hora de chegada, vale o momento do registro.")
	@ApiResponse(responseCode = "201", description = "Chegada registrada; o cabeçalho Location aponta para o atendimento")
	@ApiResponse(responseCode = "400", description = "Dados inválidos ou chegada no futuro")
	@ApiResponse(responseCode = "404", description = "Paciente ou profissional não encontrado")
	@ApiResponse(responseCode = "422", description = "Paciente ou profissional inativo")
	public ResponseEntity<AtendimentoEmergenciaResponse> registrarChegada(
			@Valid @RequestBody AtendimentoEmergenciaRequest request) {
		AtendimentoEmergenciaResponse resposta = service.registrarChegada(request);
		return ResponseEntity.created(Localizacao.de("/emergencias/{id}", resposta.id())).body(resposta);
	}

	@GetMapping("/fila")
	@Operation(summary = "Fila de quem aguarda avaliação", description = "Prioridade mais grave primeiro e, na mesma "
			+ "prioridade, ordem de chegada; quem ainda não tem prioridade definida fica no fim.")
	@ApiResponse(responseCode = "200", description = "Fila de avaliação")
	public List<AtendimentoEmergenciaResponse> fila() {
		return service.filaDeAvaliacao();
	}

	@GetMapping
	@Operation(summary = "Lista os atendimentos de uma situação", description = "Em ordem de chegada.")
	@ApiResponse(responseCode = "200", description = "Atendimentos encontrados")
	@ApiResponse(responseCode = "400", description = "Situação ausente ou desconhecida")
	public List<AtendimentoEmergenciaResponse> listar(
			@Parameter(description = "Situação do atendimento") @RequestParam SituacaoEmergencia situacao) {
		return service.listarPorSituacao(situacao);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um atendimento de emergência pelo id")
	@ApiResponse(responseCode = "200", description = "Atendimento encontrado")
	@ApiResponse(responseCode = "404", description = "Atendimento não encontrado")
	public AtendimentoEmergenciaResponse buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PatchMapping("/{id}/definir-prioridade")
	@Operation(summary = "Define ou reclassifica a prioridade", description = "Permitido até a conclusão do atendimento.")
	@ApiResponse(responseCode = "200", description = "Prioridade definida")
	@ApiResponse(responseCode = "400", description = "Prioridade ausente ou desconhecida")
	@ApiResponse(responseCode = "404", description = "Atendimento não encontrado")
	@ApiResponse(responseCode = "422", description = "O atendimento já foi concluído")
	public AtendimentoEmergenciaResponse definirPrioridade(@PathVariable Long id,
			@Valid @RequestBody PrioridadeRequest request) {
		return service.definirPrioridade(id, request);
	}

	@PatchMapping("/{id}/iniciar")
	@Operation(summary = "Inicia o atendimento")
	@ApiResponse(responseCode = "200", description = "Atendimento iniciado")
	@ApiResponse(responseCode = "404", description = "Atendimento não encontrado")
	@ApiResponse(responseCode = "409", description = "O atendimento não está aguardando avaliação")
	@ApiResponse(responseCode = "422", description = "Prioridade não definida ou participante inativo")
	public AtendimentoEmergenciaResponse iniciar(@PathVariable Long id) {
		return service.iniciar(id);
	}

	@PatchMapping("/{id}/concluir")
	@Operation(summary = "Conclui o atendimento")
	@ApiResponse(responseCode = "200", description = "Atendimento concluído")
	@ApiResponse(responseCode = "404", description = "Atendimento não encontrado")
	@ApiResponse(responseCode = "409", description = "O atendimento não está em andamento")
	public AtendimentoEmergenciaResponse concluir(@PathVariable Long id) {
		return service.concluir(id);
	}

}
