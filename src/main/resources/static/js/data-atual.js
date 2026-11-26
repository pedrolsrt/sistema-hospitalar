// Data de hoje, usada no topo de todas as telas e no painel. Os elementos <time data-hoje> recebem a data por
// extenso, e as telas que consultam a API pelo dia usam hojeIso().
var DataAtual = (function () {
	'use strict';

	// Data local no formato AAAA-MM-DD; toISOString() sozinho usaria o fuso UTC e erraria o dia à noite.
	function hojeIso() {
		var agora = new Date();
		agora.setMinutes(agora.getMinutes() - agora.getTimezoneOffset());
		return agora.toISOString().slice(0, 10);
	}

	function porExtenso() {
		return new Date().toLocaleDateString('pt-BR', {
			weekday: 'long',
			day: 'numeric',
			month: 'long',
			year: 'numeric'
		});
	}

	document.querySelectorAll('time[data-hoje]').forEach(function (elemento) {
		elemento.dateTime = hojeIso();
		elemento.textContent = porExtenso();
	});

	return { hojeIso: hojeIso, porExtenso: porExtenso };
})();
