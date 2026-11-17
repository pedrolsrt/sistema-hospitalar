package br.pucminas.hospital.exception;

/**
 * A categoria ou a área do profissional não permite a operação pedida: consulta especializada sem ser
 * especialista da área, procedimento cirúrgico sem ser cirurgião ou encaminhamento sem ser clínico geral.
 * Respondida com 422.
 */
public class ProfissionalIncompativelException extends RegraNegocioException {

	public ProfissionalIncompativelException(String mensagem) {
		super(mensagem);
	}

}
