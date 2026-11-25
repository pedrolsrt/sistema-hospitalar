// Valores dos enums para as listas de opções das telas. O GET /dominios é feito uma vez só por página, mesmo
// que mais de um trecho da tela peça os valores.
var Dominios = (function () {
	'use strict';

	var carregamento = null;

	function carregar() {
		if (!carregamento) {
			carregamento = Api.requisitar('/dominios').catch(function (erro) {
				// Uma falha não fica guardada: a próxima chamada tenta de novo.
				carregamento = null;
				throw erro;
			});
		}
		return carregamento;
	}

	// Acrescenta uma opção para cada valor, depois das que já existem no campo (como "Selecione").
	function preencher(campo, valores, rotulo) {
		valores.forEach(function (valor) {
			var opcao = document.createElement('option');
			opcao.value = valor.codigo;
			opcao.textContent = rotulo ? rotulo(valor) : valor.descricao;
			campo.appendChild(opcao);
		});
	}

	return { carregar: carregar, preencher: preencher };
})();
