package br.pucminas.hospital.exception;

/**
 * Internação pedida em um quarto que já está na capacidade máxima. Respondida com 409.
 */
public class QuartoSemVagasException extends RegraNegocioException {

	public QuartoSemVagasException(String numeroQuarto) {
		super("O quarto " + numeroQuarto + " não possui vagas disponíveis.");
	}

}
