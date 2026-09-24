/* Comportamento mínimo do protótipo: nenhum formulário envia dados na Sprint 1. */
(function () {
	'use strict';

	var MENSAGEM = 'Protótipo: funcionalidade disponível na próxima sprint.';
	var temporizador;

	function mostrarAviso() {
		var aviso = document.getElementById('aviso-prototipo');
		// Limpar o texto antes faz o leitor de tela anunciar a mensagem mesmo quando ela se repete.
		aviso.textContent = '';
		window.setTimeout(function () {
			aviso.textContent = MENSAGEM;
			aviso.classList.add('visivel');
		}, 50);
		window.clearTimeout(temporizador);
		temporizador = window.setTimeout(function () {
			aviso.classList.remove('visivel');
		}, 6000);
	}

	document.addEventListener('DOMContentLoaded', function () {
		document.querySelectorAll('form').forEach(function (formulario) {
			formulario.addEventListener('submit', function (evento) {
				evento.preventDefault();
				mostrarAviso();
			});
		});
		document.querySelectorAll('[data-prototipo]').forEach(function (botao) {
			botao.addEventListener('click', mostrarAviso);
		});
	});
})();
