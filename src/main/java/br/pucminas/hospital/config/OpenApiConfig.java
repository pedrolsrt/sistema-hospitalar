package br.pucminas.hospital.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String SCHEMA_PROBLEMA = "Problema";

	@Bean
	public OpenAPI hospitalOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Sistema de Informação Hospitalar")
				.version("0.0.1")
				.description("API REST para gestão hospitalar: pacientes, profissionais de saúde, quartos, "
						+ "consultas, internações, emergências, procedimentos cirúrgicos e histórico médico. "
						+ "Os erros seguem o formato ProblemDetail (RFC 9457), com mensagens em português."));
	}

	/**
	 * Toda resposta de erro tem o mesmo formato (o do TratadorDeErros). Em vez de repeti-lo em cada
	 * {@code @ApiResponse} dos controllers, que declaram só o código e a descrição, o esquema é aplicado
	 * aqui a todas as respostas 4xx e 5xx, e o 500 é acrescentado a todas as operações.
	 */
	@Bean
	public OpenApiCustomizer respostasDeErro() {
		return openApi -> {
			if (openApi.getComponents() == null) {
				openApi.setComponents(new Components());
			}
			openApi.getComponents().addSchemas(SCHEMA_PROBLEMA, esquemaProblema());
			openApi.getPaths().values().forEach(caminho -> caminho.readOperations().forEach(this::documentarErros));
		};
	}

	private void documentarErros(Operation operacao) {
		operacao.getResponses().computeIfAbsent("500",
				codigo -> new ApiResponse().description("Erro inesperado; os detalhes ficam só no log do servidor"));
		operacao.getResponses().forEach((codigo, resposta) -> {
			if (codigo.startsWith("4") || codigo.startsWith("5")) {
				resposta.setContent(new Content().addMediaType("application/problem+json",
						new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + SCHEMA_PROBLEMA))));
			}
		});
	}

	private static Schema<?> esquemaProblema() {
		Schema<?> campo = new ObjectSchema()
				.addProperty("campo", new StringSchema().example("cpf"))
				.addProperty("mensagem", new StringSchema().example("O CPF informado não é válido."));
		return new ObjectSchema()
				.description("Erro no formato ProblemDetail (RFC 9457)")
				.addProperty("type", new StringSchema().example("about:blank"))
				.addProperty("title", new StringSchema().example("Recurso não encontrado"))
				.addProperty("status", new IntegerSchema().example(404))
				.addProperty("detail", new StringSchema().example("Paciente não encontrado (id 9)."))
				.addProperty("instance", new StringSchema().example("/pacientes/9"))
				.addProperty("campos", new ArraySchema().items(campo)
						.description("Campos inválidos; presente apenas nas respostas 400 de validação"));
	}

}
