package br.pucminas.hospital.controller;

import br.pucminas.hospital.dto.ConsultaResponse;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoResponse;
import br.pucminas.hospital.dto.ProfissionalSaudeAtualizacaoRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeResponse;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.service.ConsultaService;
import br.pucminas.hospital.service.ProcedimentoCirurgicoService;
import br.pucminas.hospital.service.ProfissionalSaudeService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profissionais")
@Tag(name = "Profissionais de saúde", description = "Clínicos gerais, médicos especialistas e cirurgiões, com a "
		+ "agenda de consultas e os procedimentos realizados.")
public class ProfissionalSaudeController {

	private final ProfissionalSaudeService service;
	private final ConsultaService consultaService;
	private final ProcedimentoCirurgicoService procedimentoService;

	public ProfissionalSaudeController(ProfissionalSaudeService service, ConsultaService consultaService,
			ProcedimentoCirurgicoService procedimentoService) {
		this.service = service;
		this.consultaService = consultaService;
		this.procedimentoService = procedimentoService;
	}

	@GetMapping
	@Operation(summary = "Lista os profissionais", description = "Em ordem de nome. Com área, lista os profissionais "
			+ "ativos que atuam nela: especialistas pela área de atuação e cirurgiões pela área cirúrgica.")
	@ApiResponse(responseCode = "200", description = "Profissionais encontrados")
	@ApiResponse(responseCode = "400", description = "Área de atuação desconhecida")
	public List<ProfissionalSaudeResponse> listar(
			@Parameter(description = "Se verdadeiro, lista só os profissionais ativos")
			@RequestParam(defaultValue = "false") boolean ativos,
			@Parameter(description = "Área de atuação; com ela, a listagem traz só profissionais ativos")
			@RequestParam(required = false) AreaAtuacao area) {
		return area != null ? service.listarPorArea(area) : service.listar(ativos);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um profissional pelo id")
	@ApiResponse(responseCode = "200", description = "Profissional encontrado")
	@ApiResponse(responseCode = "404", description = "Profissional não encontrado")
	public ProfissionalSaudeResponse buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra um profissional", description = "A categoria define o tipo de profissional. A área "
			+ "de atuação é obrigatória para médico especialista e cirurgião e não deve ser informada para clínico geral.")
	@ApiResponse(responseCode = "201", description = "Profissional cadastrado; o cabeçalho Location aponta para ele")
	@ApiResponse(responseCode = "400", description = "Dados inválidos ou área incompatível com a categoria")
	@ApiResponse(responseCode = "409", description = "Já existe profissional com o registro informado")
	public ResponseEntity<ProfissionalSaudeResponse> cadastrar(@Valid @RequestBody ProfissionalSaudeRequest request) {
		ProfissionalSaudeResponse resposta = service.cadastrar(request);
		return ResponseEntity.created(Localizacao.de("/profissionais/{id}", resposta.id())).body(resposta);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza os dados de um profissional",
			description = "Nome, telefone e e-mail. Registro, categoria e área não podem ser alterados.")
	@ApiResponse(responseCode = "200", description = "Profissional atualizado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Profissional não encontrado")
	public ProfissionalSaudeResponse atualizar(@PathVariable Long id,
			@Valid @RequestBody ProfissionalSaudeAtualizacaoRequest request) {
		return service.atualizar(id, request);
	}

	@PatchMapping("/{id}/inativar")
	@Operation(summary = "Inativa um profissional", description = "Não é permitido com consultas, emergências, "
			+ "procedimentos ou internações ativos sob sua responsabilidade. Inativar um profissional já inativo não tem efeito.")
	@ApiResponse(responseCode = "200", description = "Profissional inativo")
	@ApiResponse(responseCode = "404", description = "Profissional não encontrado")
	@ApiResponse(responseCode = "422", description = "O profissional tem atendimentos ativos; a mensagem informa quais")
	public ProfissionalSaudeResponse inativar(@PathVariable Long id) {
		return service.inativar(id);
	}

	@PatchMapping("/{id}/reativar")
	@Operation(summary = "Reativa um profissional", description = "Reativar um profissional já ativo não tem efeito.")
	@ApiResponse(responseCode = "200", description = "Profissional ativo")
	@ApiResponse(responseCode = "404", description = "Profissional não encontrado")
	public ProfissionalSaudeResponse reativar(@PathVariable Long id) {
		return service.reativar(id);
	}

	@GetMapping("/{id}/agenda")
	@Operation(summary = "Agenda do profissional em uma data",
			description = "Consultas em qualquer situação, em ordem de horário.")
	@ApiResponse(responseCode = "200", description = "Consultas do dia")
	@ApiResponse(responseCode = "400", description = "Data ausente ou inválida")
	@ApiResponse(responseCode = "404", description = "Profissional não encontrado")
	public List<ConsultaResponse> agenda(@PathVariable Long id,
			@Parameter(description = "Data no formato AAAA-MM-DD", example = "2030-01-15")
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
		return consultaService.agendaDoProfissional(id, data);
	}

	@GetMapping("/{id}/procedimentos")
	@Operation(summary = "Procedimentos realizados por um cirurgião",
			description = "Procedimentos concluídos, do mais recente para o mais antigo.")
	@ApiResponse(responseCode = "200", description = "Procedimentos realizados")
	@ApiResponse(responseCode = "404", description = "Profissional não encontrado")
	public List<ProcedimentoCirurgicoResponse> procedimentosRealizados(@PathVariable Long id) {
		return procedimentoService.listarRealizadosPorCirurgiao(id);
	}

}
