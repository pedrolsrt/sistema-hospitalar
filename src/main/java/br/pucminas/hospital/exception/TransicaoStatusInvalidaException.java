package br.pucminas.hospital.exception;

import br.pucminas.hospital.model.StatusConsulta;

public class TransicaoStatusInvalidaException extends RegraNegocioException {

	public TransicaoStatusInvalidaException(String acao, StatusConsulta statusAtual) {
		super("Não é possível " + acao + " uma consulta " + statusAtual.name().toLowerCase()
				+ ". Apenas consultas agendadas podem ser realizadas ou canceladas.");
	}

}
