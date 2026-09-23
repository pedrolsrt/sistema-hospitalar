package br.pucminas.hospital.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI hospitalOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Sistema de Informação Hospitalar")
				.version("0.0.1")
				.description("API REST para gestão hospitalar: pacientes, profissionais de saúde, "
						+ "consultas, internações, quartos e histórico médico."));
	}

}
