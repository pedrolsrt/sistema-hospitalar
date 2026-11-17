package br.pucminas.hospital.exception;

/**
 * O horário pedido não está livre: já existe outro atendimento ativo que se sobrepõe a ele. Respondida com 409.
 */
public class AtendimentoIndisponivelException extends RegraNegocioException {

	public AtendimentoIndisponivelException(String mensagem) {
		super(mensagem);
	}

}
