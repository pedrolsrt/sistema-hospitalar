package br.pucminas.hospital.exception;

/**
 * Tentativa de levar um atendimento a uma situação que a sua máquina de estados não permite.
 */
public class TransicaoSituacaoInvalidaException extends RegraNegocioException {

	public TransicaoSituacaoInvalidaException(String mensagem) {
		super(mensagem);
	}

}
