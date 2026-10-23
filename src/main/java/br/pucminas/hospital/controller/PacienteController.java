package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.HistoricoMedicoResponse;
import br.pucminas.hospital.dto.PacienteAtualizacaoRequest;
import br.pucminas.hospital.dto.PacienteRequest;
import br.pucminas.hospital.dto.PacienteResponse;
import br.pucminas.hospital.service.HistoricoMedicoService;
import br.pucminas.hospital.service.PacienteService;
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
@RequestMapping("/pacientes")
@Tag(name = "Pacientes", description = "Cadastro de pacientes e histórico médico. Pacientes não são excluídos: "
		+ "são inativados, para preservar o histórico.")
public class PacienteController {

	private final PacienteService service;
	private final HistoricoMedicoService historicoService;

	public PacienteController(PacienteService service, HistoricoMedicoService historicoService) {
		this.service = service;
		this.historicoService = historicoService;
	}

	@GetMapping
	@Operation(summary = "Lista os pacientes", description = "Em ordem de nome; todos ou só os ativos.")
	@ApiResponse(responseCode = "200", description = "Pacientes encontrados")
	public List<PacienteResponse> listar(
			@Parameter(description = "Se verdadeiro, lista só os pacientes ativos")
			@RequestParam(defaultValue = "false") boolean ativos) {
		return service.listar(ativos);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um paciente pelo id")
	@ApiResponse(responseCode = "200", description = "Paciente encontrado")
	@ApiResponse(responseCode = "404", description = "Paciente não encontrado")
	public PacienteResponse buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra um paciente", description = "O CPF é aceito com ou sem pontuação.")
	@ApiResponse(responseCode = "201", description = "Paciente cadastrado; o cabeçalho Location aponta para ele")
	@ApiResponse(responseCode = "400", description = "Dados inválidos (por exemplo, CPF, e-mail ou data de nascimento)")
	@ApiResponse(responseCode = "409", description = "Já existe paciente com o CPF informado")
	public ResponseEntity<PacienteResponse> cadastrar(@Valid @RequestBody PacienteRequest request) {
		PacienteResponse resposta = service.cadastrar(request);
		return ResponseEntity.created(Localizacao.de("/pacientes/{id}", resposta.id())).body(resposta);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza os dados de um paciente", description = "O CPF não pode ser alterado.")
	@ApiResponse(responseCode = "200", description = "Paciente atualizado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Paciente não encontrado")
	public PacienteResponse atualizar(@PathVariable Long id, @Valid @RequestBody PacienteAtualizacaoRequest request) {
		return service.atualizar(id, request);
	}

	@PatchMapping("/{id}/inativar")
	@Operation(summary = "Inativa um paciente", description = "Não é permitido com internação ativa ou com consulta, "
			+ "emergência ou procedimento em andamento. Inativar um paciente já inativo não tem efeito.")
	@ApiResponse(responseCode = "200", description = "Paciente inativo")
	@ApiResponse(responseCode = "404", description = "Paciente não encontrado")
	@ApiResponse(responseCode = "422", description = "O paciente tem atendimentos ativos; a mensagem informa quais")
	public PacienteResponse inativar(@PathVariable Long id) {
		return service.inativar(id);
	}

	@PatchMapping("/{id}/reativar")
	@Operation(summary = "Reativa um paciente", description = "Reativar um paciente já ativo não tem efeito.")
	@ApiResponse(responseCode = "200", description = "Paciente ativo")
	@ApiResponse(responseCode = "404", description = "Paciente não encontrado")
	public PacienteResponse reativar(@PathVariable Long id) {
		return service.reativar(id);
	}

	@GetMapping("/{id}/historico")
	@Operation(summary = "Histórico médico do paciente", description = "Todos os atendimentos (consultas, "
			+ "internações, emergências e procedimentos), em ordem cronológica, com a modalidade e os dados de cada um.")
	@ApiResponse(responseCode = "200", description = "Histórico do paciente")
	@ApiResponse(responseCode = "404", description = "Paciente não encontrado")
	public HistoricoMedicoResponse historico(@PathVariable Long id) {
		return historicoService.montar(id);
	}

}
