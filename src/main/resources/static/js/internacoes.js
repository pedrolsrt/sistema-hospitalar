(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var filtro = document.getElementById('form-filtro');
	var corpo = document.getElementById('lista-internacoes');
	var aviso = document.getElementById('aviso-api');
	var formInternacao = document.getElementById('form-internacao');
	var campoQuarto = formInternacao.elements.quartoId;
	var painelAcao = document.getElementById('acao-internacao');
	var formAlta = document.getElementById('form-alta');
	var formObservacoes = document.getElementById('form-observacoes');
	var situacoes = {};
	var internacaoEmAcao = null;
	var temporizadorAviso;

	// Título, legenda e ordem de cada lista; a ordem é a que a API devolve.
	var listas = {
		'true': { titulo: 'Internações em andamento', legenda: 'Internações sem alta registrada',
			ordem: 'Em ordem de entrada.' },
		'false': { titulo: 'Internações encerradas', legenda: 'Internações com alta registrada',
			ordem: 'Da alta mais recente para a mais antiga.' }
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
		celula.textContent = valor || '—';
		if (classe) {
			celula.className = classe;
		}
		linha.appendChild(celula);
		return celula;
	}

	function criarBotao(texto, internacao, acao) {
		var botao = document.createElement('button');
		botao.type = 'button';
		botao.className = 'botao botao-secundario botao-pequeno';
		botao.textContent = texto;
		botao.setAttribute('aria-label', texto + ': internação de ' + internacao.paciente.nome);
		botao.addEventListener('click', acao);
		return botao;
	}

	function adicionarOpcao(campo, valor, texto) {
		var opcao = document.createElement('option');
		opcao.value = valor;
		opcao.textContent = texto;
		campo.appendChild(opcao);
	}

	// Mantém só a primeira opção ("Selecione") e acrescenta as novas.
	function trocarOpcoes(campo, itens, texto) {
		while (campo.options.length > 1) {
			campo.remove(1);
		}
		itens.forEach(function (item) {
			adicionarOpcao(campo, item.id, texto(item));
		});
	}

	function hoje() {
		var agora = new Date();
		agora.setMinutes(agora.getMinutes() - agora.getTimezoneOffset());
		return agora.toISOString().slice(0, 10);
	}

	function formatarData(data) {
		return data ? new Date(data + 'T00:00:00').toLocaleDateString('pt-BR') : '';
	}

	function descreverQuarto(quarto) {
		return quarto.numero + ' · ' + quarto.tipo.descricao;
	}

	function descreverProfissional(profissional) {
		return profissional.nome + ' (' + profissional.categoria.descricao
			+ (profissional.areaAtuacao ? ' · ' + profissional.areaAtuacao.descricao : '') + ')';
	}

	function descreverVagas(quarto) {
		var vagas = quarto.capacidadeMaxima - quarto.ocupacaoAtual;
		return descreverQuarto(quarto) + ', ' + quarto.andar + 'º andar (' + vagas
			+ (vagas === 1 ? ' vaga)' : ' vagas)');
	}

	function preencherTabela(internacoes) {
		corpo.replaceChildren();
		if (internacoes.length === 0) {
			var linhaVazia = document.createElement('tr');
			var mensagem = criarCelula(linhaVazia, 'Nenhuma internação encontrada.');
			mensagem.colSpan = 9;
			corpo.appendChild(linhaVazia);
			return;
		}

		internacoes.forEach(function (internacao) {
			var linha = document.createElement('tr');
			criarCelula(linha, internacao.paciente.nome, 'quebra');
			criarCelula(linha, internacao.profissional.nome, 'quebra');
			criarCelula(linha, descreverQuarto(internacao.quarto));
			criarCelula(linha, formatarData(internacao.dataEntrada));
			criarCelula(linha, formatarData(internacao.dataPrevistaAlta));
			criarCelula(linha, formatarData(internacao.dataEfetivaAlta));

			var situacao = situacoes[internacao.ativa ? 'ATIVA' : 'ENCERRADA'];
			var celulaSituacao = criarCelula(linha, '');
			if (situacao) {
				celulaSituacao.replaceChildren();
				var etiqueta = document.createElement('span');
				etiqueta.className = 'etiqueta etiqueta-' + situacao.codigo.toLowerCase();
				etiqueta.textContent = situacao.descricao;
				celulaSituacao.appendChild(etiqueta);
			}

			criarCelula(linha, internacao.observacoes, 'texto-longo');

			var celulaAcoes = criarCelula(linha, '');
			celulaAcoes.replaceChildren();
			var acoes = document.createElement('div');
			acoes.className = 'acoes';
			if (internacao.ativa) {
				acoes.appendChild(criarBotao('Registrar alta', internacao, function () {
					abrirAcao(internacao, formAlta);
				}));
			}
			acoes.appendChild(criarBotao('Observações', internacao, function () {
				abrirAcao(internacao, formObservacoes);
			}));
			celulaAcoes.appendChild(acoes);
			corpo.appendChild(linha);
		});
	}

	async function carregarInternacoes() {
		var lista = listas[filtro.elements.ativas.value];
		document.getElementById('titulo-lista').textContent = lista.titulo;
		document.getElementById('legenda-lista').textContent = lista.legenda;
		document.getElementById('ordem-lista').textContent = lista.ordem;
		corpo.replaceChildren();
		try {
			preencherTabela(await requisitar('/internacoes?ativas=' + filtro.elements.ativas.value));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as internações. ' + erro.message);
		}
	}

	// As vagas mudam a cada internação e alta, então a lista de quartos é recarregada depois de cada uma.
	async function carregarQuartos() {
		var selecionado = campoQuarto.value;
		try {
			var quartos = await requisitar('/quartos?comVaga=true');
			trocarOpcoes(campoQuarto, quartos, descreverVagas);
			campoQuarto.options[0].textContent = quartos.length ? 'Selecione o quarto' : 'Nenhum quarto com vaga';
			campoQuarto.value = selecionado;
			if (campoQuarto.selectedIndex < 0) {
				campoQuarto.value = '';
			}
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os quartos. ' + erro.message);
		}
	}

	// Alta e observações usam o mesmo painel, com o formulário de cada ação.
	function abrirAcao(internacao, formulario) {
		internacaoEmAcao = internacao;
		formAlta.hidden = formulario !== formAlta;
		formObservacoes.hidden = formulario !== formObservacoes;
		if (formulario === formAlta) {
			document.getElementById('titulo-acao').textContent = 'Registrar alta';
			formAlta.elements.dataEfetivaAlta.min = internacao.dataEntrada;
			formAlta.elements.dataEfetivaAlta.max = hoje();
			formAlta.elements.dataEfetivaAlta.value = hoje();
		} else {
			document.getElementById('titulo-acao').textContent = 'Registrar observações';
			formObservacoes.elements.texto.value = internacao.observacoes || '';
		}
		document.getElementById('resumo-acao').textContent = 'Internação de ' + internacao.paciente.nome
			+ ' no quarto ' + descreverQuarto(internacao.quarto) + ', desde ' + formatarData(internacao.dataEntrada)
			+ '.';
		painelAcao.hidden = false;
		painelAcao.scrollIntoView({ behavior: 'smooth' });
		formulario.elements[0].focus();
	}

	function fecharAcao() {
		internacaoEmAcao = null;
		painelAcao.hidden = true;
	}

	formAlta.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var data = formAlta.elements.dataEfetivaAlta.value;
		if (!window.confirm('Registrar a alta de ' + internacaoEmAcao.paciente.nome + ' em ' + formatarData(data)
				+ '? A vaga do quarto ' + internacaoEmAcao.quarto.numero + ' será liberada.')) {
			return;
		}
		try {
			await requisitar('/internacoes/' + internacaoEmAcao.id + '/registrar-alta', {
				method: 'PATCH',
				body: JSON.stringify({ dataEfetivaAlta: data })
			});
			mostrarAviso('Alta registrada com sucesso. A vaga do quarto foi liberada.');
			fecharAcao();
			await Promise.all([carregarInternacoes(), carregarQuartos()]);
		} catch (erro) {
			mostrarAviso('Não foi possível registrar a alta. ' + erro.message);
		}
	});

	formObservacoes.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		try {
			await requisitar('/internacoes/' + internacaoEmAcao.id + '/registrar-observacoes', {
				method: 'PATCH',
				body: JSON.stringify({ texto: formObservacoes.elements.texto.value })
			});
			mostrarAviso('Observações registradas com sucesso.');
			fecharAcao();
			await carregarInternacoes();
		} catch (erro) {
			mostrarAviso('Não foi possível registrar as observações. ' + erro.message);
		}
	});

	painelAcao.querySelectorAll('[data-fechar]').forEach(function (botao) {
		botao.addEventListener('click', fecharAcao);
	});

	filtro.addEventListener('submit', function (evento) {
		evento.preventDefault();
		fecharAcao();
		carregarInternacoes();
	});

	formInternacao.elements.dataEntrada.addEventListener('change', function () {
		formInternacao.elements.dataPrevistaAlta.min = formInternacao.elements.dataEntrada.value;
	});

	formInternacao.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var dados = new FormData(formInternacao);
		var observacoes = dados.get('observacoes').trim();
		try {
			await requisitar('/internacoes', {
				method: 'POST',
				body: JSON.stringify({
					pacienteId: Number(dados.get('pacienteId')),
					profissionalId: Number(dados.get('profissionalId')),
					quartoId: Number(dados.get('quartoId')),
					dataEntrada: dados.get('dataEntrada'),
					dataPrevistaAlta: dados.get('dataPrevistaAlta'),
					observacoes: observacoes || null
				})
			});
			formInternacao.reset();
			mostrarAviso('Internação registrada com sucesso.');
			filtro.elements.ativas.value = 'true';
			await Promise.all([carregarInternacoes(), carregarQuartos()]);
		} catch (erro) {
			mostrarAviso('Não foi possível registrar a internação. ' + erro.message);
			// Uma recusa por falta de vaga mostra que a lista de quartos estava desatualizada.
			await carregarQuartos();
		}
	});

	formInternacao.addEventListener('reset', function () {
		// Os campos só voltam ao valor inicial depois deste evento.
		window.setTimeout(function () {
			formInternacao.elements.dataEntrada.value = hoje();
			formInternacao.elements.dataPrevistaAlta.min = hoje();
		});
	});

	async function iniciar() {
		formInternacao.elements.dataEntrada.value = hoje();
		formInternacao.elements.dataPrevistaAlta.min = hoje();
		try {
			var dominios = await Dominios.carregar();
			dominios.situacoesInternacao.forEach(function (situacao) {
				situacoes[situacao.codigo] = situacao;
			});
			var opcoes = await Promise.all([requisitar('/pacientes?ativos=true'),
				requisitar('/profissionais?ativos=true')]);
			trocarOpcoes(formInternacao.elements.pacienteId, opcoes[0], function (paciente) {
				return paciente.nome;
			});
			trocarOpcoes(formInternacao.elements.profissionalId, opcoes[1], descreverProfissional);
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as opções da tela. ' + erro.message);
		}
		await Promise.all([carregarInternacoes(), carregarQuartos()]);
	}

	iniciar();
})();
