package br.pucminas.hospital.exception;

/**
 * Um dado que deve ser único (CPF, registro profissional, número do quarto) já está cadastrado.
 * Respondida com 409.
 */
public class DadoJaCadastradoException extends RegraNegocioException {

	public DadoJaCadastradoException(String mensagem) {
		super(mensagem);
	}

}
