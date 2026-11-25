// Chamadas à API usadas por todas as telas. Os erros chegam no formato ProblemDetail e viram uma mensagem
// pronta para mostrar ao usuário.
var Api = (function () {
	'use strict';

	async function requisitar(url, opcoes) {
		var resposta;
		try {
			resposta = await fetch(url, Object.assign({
				headers: { 'Content-Type': 'application/json' }
			}, opcoes || {}));
		} catch (erro) {
			throw new Error('Não foi possível conectar à API. Verifique se o servidor está disponível.');
		}

		if (!resposta.ok) {
			var texto = await resposta.text();
			var problema;
			try {
				problema = texto ? JSON.parse(texto) : {};
			} catch (erroDeParse) {
				throw new Error('A API respondeu com erro (' + resposta.status + ').');
			}
			// Nos erros de validação, cada mensagem já diz qual é o campo.
			var mensagensCampos = Array.isArray(problema.campos)
				? problema.campos.map(function (campo) {
					return campo.mensagem;
				})
				: [];
			throw new Error(mensagensCampos.length
				? mensagensCampos.join(' ')
				: problema.detail || problema.title || 'A API respondeu com erro (' + resposta.status + ').');
		}

		return resposta.status === 204 ? null : resposta.json();
	}

	return { requisitar: requisitar };
})();
