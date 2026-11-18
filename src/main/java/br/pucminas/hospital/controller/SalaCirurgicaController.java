package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.SalaCirurgicaRequest;
import br.pucminas.hospital.dto.SalaCirurgicaResponse;
import br.pucminas.hospital.service.SalaCirurgicaService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/salas-cirurgicas")
@Tag(name = "Salas cirúrgicas", description = "Salas onde os procedimentos são realizados. Salas não são excluídas: "
		+ "são inativadas e deixam de receber agendamentos.")
public class SalaCirurgicaController {

	private final SalaCirurgicaService service;

	public SalaCirurgicaController(SalaCirurgicaService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Lista as salas cirúrgicas", description = "Em ordem de identificação; todas ou só as ativas.")
	@ApiResponse(responseCode = "200", description = "Salas encontradas")
	public List<SalaCirurgicaResponse> listar(
			@Parameter(description = "Se verdadeiro, lista só as salas ativas")
			@RequestParam(defaultValue = "false") boolean ativas) {
		return service.listar(ativas);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma sala cirúrgica pelo id")
	@ApiResponse(responseCode = "200", description = "Sala encontrada")
	@ApiResponse(responseCode = "404", description = "Sala não encontrada")
	public SalaCirurgicaResponse buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra uma sala cirúrgica", description = "A sala é cadastrada ativa.")
	@ApiResponse(responseCode = "201", description = "Sala cadastrada; o cabeçalho Location aponta para ela")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "409", description = "Já existe sala com a identificação informada")
	public ResponseEntity<SalaCirurgicaResponse> cadastrar(@Valid @RequestBody SalaCirurgicaRequest request) {
		SalaCirurgicaResponse resposta = service.cadastrar(request);
		return ResponseEntity.created(Localizacao.de("/salas-cirurgicas/{id}", resposta.id())).body(resposta);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza a identificação de uma sala cirúrgica")
	@ApiResponse(responseCode = "200", description = "Sala atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Sala não encontrada")
	@ApiResponse(responseCode = "409", description = "A identificação informada pertence a outra sala")
	public SalaCirurgicaResponse atualizar(@PathVariable Long id, @Valid @RequestBody SalaCirurgicaRequest request) {
		return service.atualizar(id, request);
	}

	@PatchMapping("/{id}/inativar")
	@Operation(summary = "Inativa uma sala cirúrgica", description = "Não é permitido enquanto houver procedimento "
			+ "agendado ou em andamento na sala. Inativar uma sala já inativa não tem efeito.")
	@ApiResponse(responseCode = "200", description = "Sala inativa")
	@ApiResponse(responseCode = "404", description = "Sala não encontrada")
	@ApiResponse(responseCode = "422", description = "A sala tem procedimento agendado ou em andamento")
	public SalaCirurgicaResponse inativar(@PathVariable Long id) {
		return service.inativar(id);
	}

	@PatchMapping("/{id}/reativar")
	@Operation(summary = "Reativa uma sala cirúrgica", description = "Reativar uma sala já ativa não tem efeito.")
	@ApiResponse(responseCode = "200", description = "Sala ativa")
	@ApiResponse(responseCode = "404", description = "Sala não encontrada")
	public SalaCirurgicaResponse reativar(@PathVariable Long id) {
		return service.reativar(id);
	}

}
