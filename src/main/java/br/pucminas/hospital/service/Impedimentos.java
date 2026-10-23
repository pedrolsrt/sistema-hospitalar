package br.pucminas.hospital.service;

import br.pucminas.hospital.exception.RegraNegocioException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reúne o que impede uma operação para informar tudo de uma vez, e não só o primeiro motivo.
 */
final class Impedimentos {

	private final List<String> motivos = new ArrayList<>();

	Impedimentos se(boolean condicao, String motivo) {
		if (condicao) {
			motivos.add(motivo);
		}
		return this;
	}

	/**
	 * @param operacao início da frase, por exemplo "O paciente Maria Silva não pode ser inativado porque tem"
	 * @throws RegraNegocioException se houver algum impedimento, listando todos
	 */
	void exigirNenhum(String operacao) {
		if (!motivos.isEmpty()) {
			throw new RegraNegocioException(operacao + ": " + String.join("; ", motivos) + ".");
		}
	}

}
