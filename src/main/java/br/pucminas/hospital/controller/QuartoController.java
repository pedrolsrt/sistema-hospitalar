package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.QuartoRequest;
import br.pucminas.hospital.dto.QuartoResponse;
import br.pucminas.hospital.model.TipoQuarto;
import br.pucminas.hospital.service.QuartoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quartos")
@Tag(name = "Quartos", description = "Quartos e a sua ocupação. Cada tipo tem uma capacidade máxima permitida: "
		+ "enfermaria até 6 leitos, apartamento até 2 e UTI 1.")
public class QuartoController {

	private final QuartoService service;

	public QuartoController(QuartoService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Lista os quartos", description = "Em ordem de número; os filtros podem ser combinados.")
	@ApiResponse(responseCode = "200", description = "Quartos encontrados")
	@ApiResponse(responseCode = "400", description = "Tipo de quarto desconhecido")
	public List<QuartoResponse> listar(
			@Parameter(description = "Só quartos deste tipo") @RequestParam(required = false) TipoQuarto tipo,
			@Parameter(description = "Se verdadeiro, só quartos com vaga")
			@RequestParam(defaultValue = "false") boolean comVaga) {
		return service.listar(tipo, comVaga);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um quarto pelo id")
	@ApiResponse(responseCode = "200", description = "Quarto encontrado")
	@ApiResponse(responseCode = "404", description = "Quarto não encontrado")
	public QuartoResponse buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra um quarto")
	@ApiResponse(responseCode = "201", description = "Quarto cadastrado; o cabeçalho Location aponta para ele")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "409", description = "Já existe quarto com o número informado")
	@ApiResponse(responseCode = "422", description = "Capacidade acima do limite do tipo")
	public ResponseEntity<QuartoResponse> cadastrar(@Valid @RequestBody QuartoRequest request) {
		QuartoResponse resposta = service.cadastrar(request);
		return ResponseEntity.created(Localizacao.de("/quartos/{id}", resposta.id())).body(resposta);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um quarto", description = "Tipo e capacidade são validados juntos; a capacidade não "
			+ "pode ficar abaixo da ocupação atual.")
	@ApiResponse(responseCode = "200", description = "Quarto atualizado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Quarto não encontrado")
	@ApiResponse(responseCode = "409", description = "O número informado pertence a outro quarto")
	@ApiResponse(responseCode = "422", description = "Capacidade acima do limite do tipo ou abaixo da ocupação")
	public QuartoResponse atualizar(@PathVariable Long id, @Valid @RequestBody QuartoRequest request) {
		return service.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove um quarto",
			description = "Só é permitido para quartos que nunca receberam internação, para preservar o histórico.")
	@ApiResponse(responseCode = "204", description = "Quarto removido")
	@ApiResponse(responseCode = "404", description = "Quarto não encontrado")
	@ApiResponse(responseCode = "422", description = "O quarto tem internação ativa ou histórico de internações")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		service.remover(id);
		return ResponseEntity.noContent().build();
	}

}
