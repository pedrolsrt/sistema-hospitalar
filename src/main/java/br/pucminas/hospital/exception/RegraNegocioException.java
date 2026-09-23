package br.pucminas.hospital.exception;

/**
 * Violação de uma regra de negócio. Base das exceções de domínio; a mensagem é exibida ao usuário final.
 */
public class RegraNegocioException extends RuntimeException {

	public RegraNegocioException(String mensagem) {
		super(mensagem);
	}

}
