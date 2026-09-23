package br.pucminas.hospital.exception;

public class QuartoSemVagaException extends RegraNegocioException {

	public QuartoSemVagaException(String numeroQuarto) {
		super("O quarto " + numeroQuarto + " não possui vagas disponíveis.");
	}

}
