package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.Descritivel;

/**
 * Valor de um enum na resposta: o código, que o cliente envia de volta nas requisições, e a descrição
 * legível para exibir.
 */
public record CodigoDescricao(String codigo, String descricao) {

	/**
	 * @return o código e a descrição do valor, ou null se o valor for nulo
	 */
	public static CodigoDescricao de(Descritivel valor) {
		return valor == null ? null : new CodigoDescricao(valor.name(), valor.getDescricao());
	}

}
