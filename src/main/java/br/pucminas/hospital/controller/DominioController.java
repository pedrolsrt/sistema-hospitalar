package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.DominiosResponse;
import br.pucminas.hospital.service.DominioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dominios")
@Tag(name = "Domínios", description = "Valores aceitos pela API em cada lista de opções, para as telas.")
public class DominioController {

	private final DominioService service;

	public DominioController(DominioService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Lista os valores de cada enum", description = "Situações de cada modalidade, modalidades, "
			+ "categorias de profissional, áreas de atuação, tipos de quarto, prioridades de emergência, tipos de "
			+ "cirurgia e opções de ordenação, cada valor com o código, que é o que se envia à API, e a descrição "
			+ "para exibir.")
	@ApiResponse(responseCode = "200", description = "Valores de cada lista, na ordem em que foram declarados")
	public DominiosResponse listar() {
		return service.listar();
	}

}
