package br.pucminas.hospital.exception;

/**
 * Campo da requisição que não passou na validação, listado na resposta de erro 400.
 */
public record CampoInvalido(String campo, String mensagem) {
}
