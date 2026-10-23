package br.pucminas.hospital.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {

	// Os services recebem o relógio por injeção, e não chamam LocalDateTime.now() direto, para que os
	// testes fixem o "agora" das regras que dependem dele (por exemplo, consulta no passado).
	@Bean
	public Clock relogio() {
		return Clock.systemDefaultZone();
	}

}
