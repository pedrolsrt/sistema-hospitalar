(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var formulario = document.getElementById('form-sala');
	var filtro = document.getElementById('form-filtro');
	var corpo = document.getElementById('lista-salas');
	var aviso = document.getElementById('aviso-api');
	var idEmEdicao = null;
	var temporizadorAviso;

	function mostrarAviso(mensagem) {
		aviso.textContent = mensagem;
		aviso.classList.add('visivel');
		window.clearTimeout(temporizadorAviso);
		temporizadorAviso = window.setTimeout(function () {
			aviso.classList.remove('visivel');
		}, 6000);
	}

	function criarCelula(linha, valor, classe) {
		var celula = document.createElement('td');
		celula.textContent = valor || '—';
		if (classe) {
			celula.className = classe;
		}
		linha.appendChild(celula);
		return celula;
	}

	function criarBotao(texto, estilo, identificacao, acao) {
		var botao = document.createElement('button');
		botao.type = 'button';
		botao.className = 'botao ' + estilo + ' botao-pequeno';
		botao.textContent = texto;
		botao.setAttribute('aria-label', texto + ' sala ' + identificacao);
		botao.addEventListener('click', acao);
		return botao;
	}

	function preencherTabela(salas) {
		corpo.replaceChildren();
		if (salas.length === 0) {
			var linhaVazia = document.createElement('tr');
			var mensagem = criarCelula(linhaVazia, 'Nenhuma sala encontrada.');
			mensagem.colSpan = 3;
			corpo.appendChild(linhaVazia);
			return;
		}

		salas.forEach(function (sala) {
			var linha = document.createElement('tr');
			if (!sala.ativa) {
				linha.className = 'inativo';
			}
			criarCelula(linha, sala.identificacao, 'quebra');

			var celulaSituacao = criarCelula(linha, '');
			celulaSituacao.replaceChildren();
			var etiqueta = document.createElement('span');
			etiqueta.className = 'etiqueta etiqueta-' + (sala.ativa ? 'ativo' : 'inativo');
			etiqueta.textContent = sala.ativa ? 'Ativa' : 'Inativa';
			celulaSituacao.appendChild(etiqueta);

			var celulaAcoes = criarCelula(linha, '');
			celulaAcoes.replaceChildren();
			var acoes = document.createElement('div');
			acoes.className = 'acoes';
			acoes.appendChild(criarBotao('Editar', 'botao-secundario', sala.identificacao, function () {
				editar(sala);
			}));
			acoes.appendChild(criarBotao(sala.ativa ? 'Inativar' : 'Reativar',
				sala.ativa ? 'botao-perigo' : 'botao-secundario', sala.identificacao, function () {
					alterarSituacao(sala);
				}));
			celulaAcoes.appendChild(acoes);
			corpo.appendChild(linha);
		});
	}

	async function carregarSalas() {
		corpo.replaceChildren();
		try {
			preencherTabela(await requisitar('/salas-cirurgicas?ativas=' + filtro.elements.ativas.value));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as salas cirúrgicas. ' + erro.message);
		}
	}

	function editar(sala) {
		idEmEdicao = sala.id;
		formulario.elements.identificacao.value = sala.identificacao;
		document.getElementById('titulo-cadastro').textContent = 'Editar sala';
		formulario.querySelector('[type="submit"]').textContent = 'Salvar alterações';
		document.getElementById('cadastro').scrollIntoView({ behavior: 'smooth' });
		formulario.elements.identificacao.focus();
	}

	async function alterarSituacao(sala) {
		var acao = sala.ativa ? 'inativar' : 'reativar';
		try {
			await requisitar('/salas-cirurgicas/' + sala.id + '/' + acao, { method: 'PATCH' });
			mostrarAviso('Sala cirúrgica ' + (sala.ativa ? 'inativada' : 'reativada') + ' com sucesso.');
			await carregarSalas();
		} catch (erro) {
			mostrarAviso('Não foi possível alterar a situação da sala. ' + erro.message);
		}
	}

	filtro.addEventListener('submit', function (evento) {
		evento.preventDefault();
		carregarSalas();
	});
	formulario.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var sala = { identificacao: new FormData(formulario).get('identificacao') };
		var estavaEditando = idEmEdicao !== null;

		try {
			await requisitar(estavaEditando ? '/salas-cirurgicas/' + idEmEdicao : '/salas-cirurgicas', {
				method: estavaEditando ? 'PUT' : 'POST',
				body: JSON.stringify(sala)
			});
			formulario.reset();
			mostrarAviso(estavaEditando ? 'Sala atualizada com sucesso.' : 'Sala cadastrada com sucesso.');
			await carregarSalas();
		} catch (erro) {
			mostrarAviso('Não foi possível salvar a sala. ' + erro.message);
		}
	});

	formulario.addEventListener('reset', function () {
		idEmEdicao = null;
		document.getElementById('titulo-cadastro').textContent = 'Cadastrar sala';
		formulario.querySelector('[type="submit"]').textContent = 'Salvar sala';
	});

	carregarSalas();
})();
