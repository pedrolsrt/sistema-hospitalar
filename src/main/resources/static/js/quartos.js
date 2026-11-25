(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var formulario = document.getElementById('form-quarto');
	var filtro = document.getElementById('form-filtro');
	var corpo = document.getElementById('lista-quartos');
	var aviso = document.getElementById('aviso-api');
	var campoTipo = formulario.elements.tipo;
	var campoCapacidade = formulario.elements.capacidadeMaxima;
	var ajudaTipo = document.getElementById('ajuda-tipo');
	var idEmEdicao = null;
	var temporizadorAviso;
	// Código do tipo -> descrição e limite de leitos, vindos do GET /dominios.
	var tipos = {};
	// A API devolve só o código da situação do quarto.
	var situacoes = {
		DISPONIVEL: { texto: 'Disponível', classe: 'etiqueta-disponivel' },
		OCUPADO: { texto: 'Ocupado', classe: 'etiqueta-ocupado' }
	};

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
		celula.textContent = valor === null || valor === undefined || valor === '' ? '—' : valor;
		if (classe) {
			celula.className = classe;
		}
		linha.appendChild(celula);
		return celula;
	}

	function criarBotao(texto, estilo, numero, acao) {
		var botao = document.createElement('button');
		botao.type = 'button';
		botao.className = 'botao ' + estilo + ' botao-pequeno';
		botao.textContent = texto;
		botao.setAttribute('aria-label', texto + ' quarto ' + numero);
		botao.addEventListener('click', acao);
		return botao;
	}

	function leitos(quantidade) {
		return quantidade + (quantidade === 1 ? ' leito' : ' leitos');
	}

	function formatarAndar(andar) {
		return andar === 0 ? 'Térreo' : andar + 'º';
	}

	function preencherTabela(quartos) {
		corpo.replaceChildren();
		if (quartos.length === 0) {
			var linhaVazia = document.createElement('tr');
			var mensagem = criarCelula(linhaVazia, 'Nenhum quarto encontrado.');
			mensagem.colSpan = 7;
			corpo.appendChild(linhaVazia);
			return;
		}

		quartos.forEach(function (quarto) {
			var linha = document.createElement('tr');
			criarCelula(linha, quarto.numero);
			criarCelula(linha, formatarAndar(quarto.andar));
			criarCelula(linha, quarto.tipo.descricao);
			criarCelula(linha, quarto.capacidadeMaxima);
			criarCelula(linha, quarto.ocupacaoAtual);

			var celulaSituacao = criarCelula(linha, '');
			celulaSituacao.replaceChildren();
			var situacao = situacoes[quarto.situacao];
			var etiqueta = document.createElement('span');
			etiqueta.className = 'etiqueta ' + situacao.classe;
			etiqueta.textContent = situacao.texto;
			celulaSituacao.appendChild(etiqueta);

			var celulaAcoes = criarCelula(linha, '');
			celulaAcoes.replaceChildren();
			var acoes = document.createElement('div');
			acoes.className = 'acoes';
			acoes.appendChild(criarBotao('Editar', 'botao-secundario', quarto.numero, function () {
				editar(quarto);
			}));
			acoes.appendChild(criarBotao('Remover', 'botao-perigo', quarto.numero, function () {
				remover(quarto);
			}));
			celulaAcoes.appendChild(acoes);
			corpo.appendChild(linha);
		});
	}

	async function carregarQuartos() {
		var parametros = new URLSearchParams({ comVaga: filtro.elements.comVaga.value });
		if (filtro.elements.tipo.value) {
			parametros.set('tipo', filtro.elements.tipo.value);
		}
		corpo.replaceChildren();
		try {
			preencherTabela(await requisitar('/quartos?' + parametros));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os quartos. ' + erro.message);
		}
	}

	// Mostra o limite do tipo escolhido e impede, já no formulário, uma capacidade acima dele.
	function atualizarLimite() {
		var tipo = tipos[campoTipo.value];
		if (tipo) {
			ajudaTipo.textContent = tipo.descricao + ': até ' + leitos(tipo.capacidadeMaximaPermitida) + '.';
			campoCapacidade.max = tipo.capacidadeMaximaPermitida;
		} else {
			ajudaTipo.textContent = 'Escolha o tipo para ver o limite de leitos.';
			campoCapacidade.removeAttribute('max');
		}
	}

	function editar(quarto) {
		idEmEdicao = quarto.id;
		formulario.elements.numero.value = quarto.numero;
		formulario.elements.andar.value = quarto.andar;
		campoTipo.value = quarto.tipo.codigo;
		campoCapacidade.value = quarto.capacidadeMaxima;
		atualizarLimite();
		document.getElementById('titulo-cadastro').textContent = 'Editar quarto ' + quarto.numero;
		formulario.querySelector('[type="submit"]').textContent = 'Salvar alterações';
		document.getElementById('cadastro').scrollIntoView({ behavior: 'smooth' });
		formulario.elements.numero.focus();
	}

	async function remover(quarto) {
		if (!window.confirm('Remover o quarto ' + quarto.numero + '? Esta ação não pode ser desfeita.')) {
			return;
		}
		try {
			await requisitar('/quartos/' + quarto.id, { method: 'DELETE' });
			if (idEmEdicao === quarto.id) {
				formulario.reset();
			}
			mostrarAviso('Quarto ' + quarto.numero + ' removido com sucesso.');
			await carregarQuartos();
		} catch (erro) {
			mostrarAviso('Não foi possível remover o quarto. ' + erro.message);
		}
	}

	campoTipo.addEventListener('change', atualizarLimite);
	filtro.addEventListener('submit', function (evento) {
		evento.preventDefault();
		carregarQuartos();
	});
	formulario.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var dados = new FormData(formulario);
		var quarto = {
			numero: dados.get('numero'),
			andar: Number(dados.get('andar')),
			tipo: dados.get('tipo'),
			capacidadeMaxima: Number(dados.get('capacidadeMaxima'))
		};
		var estavaEditando = idEmEdicao !== null;

		try {
			await requisitar(estavaEditando ? '/quartos/' + idEmEdicao : '/quartos', {
				method: estavaEditando ? 'PUT' : 'POST',
				body: JSON.stringify(quarto)
			});
			formulario.reset();
			mostrarAviso(estavaEditando ? 'Quarto atualizado com sucesso.' : 'Quarto cadastrado com sucesso.');
			await carregarQuartos();
		} catch (erro) {
			mostrarAviso('Não foi possível salvar o quarto. ' + erro.message);
		}
	});

	formulario.addEventListener('reset', function () {
		idEmEdicao = null;
		document.getElementById('titulo-cadastro').textContent = 'Cadastrar quarto';
		formulario.querySelector('[type="submit"]').textContent = 'Salvar quarto';
		// Os campos só voltam ao valor inicial depois deste evento.
		window.setTimeout(atualizarLimite);
	});

	async function iniciar() {
		try {
			var dominios = await Dominios.carregar();
			dominios.tiposQuarto.forEach(function (tipo) {
				tipos[tipo.codigo] = tipo;
			});
			Dominios.preencher(filtro.elements.tipo, dominios.tiposQuarto);
			Dominios.preencher(campoTipo, dominios.tiposQuarto, function (tipo) {
				return tipo.descricao + ' (até ' + leitos(tipo.capacidadeMaximaPermitida) + ')';
			});
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os tipos de quarto. ' + erro.message);
		}
		await carregarQuartos();
	}

	iniciar();
})();
