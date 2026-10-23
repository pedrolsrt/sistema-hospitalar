package br.pucminas.hospital.exception;

/**
 * O recurso pedido não existe. Respondida com 404.
 */
public class RecursoNaoEncontradoException extends RegraNegocioException {

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}
