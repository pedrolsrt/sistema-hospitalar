package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.AltaRequest;
import br.pucminas.hospital.dto.InternacaoRequest;
import br.pucminas.hospital.dto.InternacaoResponse;
import br.pucminas.hospital.service.InternacaoService;
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
@RequestMapping("/internacoes")
@Tag(name = "Internações", description = "Internações e altas. A internação ocupa uma vaga do quarto, devolvida na alta.")
public class InternacaoController {

	private final InternacaoService service;

	public InternacaoController(InternacaoService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Interna um paciente", description = "Exige paciente e profissional ativos, paciente sem outra "
			+ "internação ativa, quarto com vaga e alta prevista não anterior à entrada.")
	@ApiResponse(responseCode = "201", description = "Paciente internado; o cabeçalho Location aponta para a internação")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Paciente, profissional ou quarto não encontrado")
	@ApiResponse(responseCode = "409", description = "O quarto foi alterado por outra operação ao mesmo tempo")
	@ApiResponse(responseCode = "422", description = "Paciente já internado, quarto sem vaga, participante inativo "
			+ "ou datas incoerentes")
	public ResponseEntity<InternacaoResponse> internar(@Valid @RequestBody InternacaoRequest request) {
		InternacaoResponse resposta = service.internar(request);
		return ResponseEntity.created(Localizacao.de("/internacoes/{id}", resposta.id())).body(resposta);
	}

	@GetMapping
	@Operation(summary = "Lista as internações ativas ou as encerradas", description = "Ativas em ordem de entrada; "
			+ "encerradas da alta mais recente para a mais antiga.")
	@ApiResponse(responseCode = "200", description = "Internações encontradas")
	public List<InternacaoResponse> listar(
			@Parameter(description = "Verdadeiro para as ativas (padrão), falso para as encerradas")
			@RequestParam(defaultValue = "true") boolean ativas) {
		return ativas ? service.listarAtivas() : service.listarEncerradas();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma internação pelo id")
	@ApiResponse(responseCode = "200", description = "Internação encontrada")
	@ApiResponse(responseCode = "404", description = "Internação não encontrada")
	public InternacaoResponse buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PatchMapping("/{id}/registrar-alta")
	@Operation(summary = "Registra a alta", description = "Encerra a internação e libera a vaga do quarto. A data de "
			+ "alta não pode ser anterior à entrada nem estar no futuro.")
	@ApiResponse(responseCode = "200", description = "Alta registrada")
	@ApiResponse(responseCode = "400", description = "Data ausente ou no futuro")
	@ApiResponse(responseCode = "404", description = "Internação não encontrada")
	@ApiResponse(responseCode = "422", description = "Internação já encerrada ou alta anterior à entrada")
	public InternacaoResponse registrarAlta(@PathVariable Long id, @Valid @RequestBody AltaRequest request) {
		return service.registrarAlta(id, request);
	}

}
