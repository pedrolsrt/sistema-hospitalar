package br.pucminas.hospital.mapper;

/**
 * Normalização de textos vindos das requisições, comum aos mappers.
 */
final class Textos {

	private Textos() {
	}

	static String somenteDigitos(String texto) {
		return texto.replaceAll("\\D", "");
	}

	// Campos opcionais enviados em branco são gravados como ausentes, e não como texto vazio.
	static String vazioComoNulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.strip();
	}

}
