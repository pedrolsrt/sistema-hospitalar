package br.pucminas.hospital.controller;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * URI do recurso criado, para o cabeçalho Location das respostas 201.
 */
final class Localizacao {

	private Localizacao() {
	}

	// Parte da raiz da aplicação, e não da URI da requisição, porque nem sempre o recurso criado fica
	// abaixo dela (POST /consultas/rotina cria /consultas/{id}).
	static URI de(String caminho, Long id) {
		return ServletUriComponentsBuilder.fromCurrentContextPath().path(caminho).buildAndExpand(id).toUri();
	}

}
