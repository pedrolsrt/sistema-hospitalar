package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.IndicadoresResponse;
import br.pucminas.hospital.service.IndicadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/indicadores")
@Tag(name = "Indicadores", description = "Indicadores gerenciais: consultas realizadas, atendimentos por modalidade "
		+ "e por profissional e quartos disponíveis e ocupados.")
public class IndicadorController {

	private final IndicadorService service;

	public IndicadorController(IndicadorService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Calcula os indicadores gerenciais", description = "O período é opcional e inclui as duas "
			+ "datas; vale para os indicadores de atendimentos, comparando a data de referência de cada atendimento, "
			+ "como na busca de atendimentos. Consulta realizada é consulta concluída. Os quartos mostram sempre a "
			+ "situação atual. Todas as modalidades aparecem, inclusive as sem atendimentos; os profissionais "
			+ "aparecem quando têm atendimentos no período, do maior para o menor número.")
	@ApiResponse(responseCode = "200", description = "Indicadores calculados")
	@ApiResponse(responseCode = "400", description = "Data em formato inválido")
	@ApiResponse(responseCode = "422", description = "Início do período posterior ao fim")
	public IndicadoresResponse calcular(
			@Parameter(description = "Primeira data do período, no formato AAAA-MM-DD", example = "2030-01-01")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@Parameter(description = "Última data do período, incluída, no formato AAAA-MM-DD", example = "2030-01-31")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
		return service.calcular(inicio, fim);
	}

}
