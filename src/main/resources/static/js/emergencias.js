(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var corpoFila = document.getElementById('lista-fila');
	var corpoSituacao = document.getElementById('lista-situacao');
	var filtro = document.getElementById('form-filtro');
	var aviso = document.getElementById('aviso-api');
	var formChegada = document.getElementById('form-chegada');
	var painelAcao = document.getElementById('acao-emergencia');
	var formPrioridade = document.getElementById('form-prioridade');
	var formObservacoes = document.getElementById('form-observacoes');
	var emergenciaEmAcao = null;
	var temporizadorAviso;

	// A fila já mostra quem aguarda avaliação; a segunda lista começa por quem está sendo atendido.
	var SITUACAO_INICIAL_DA_LISTA = 'EM_ATENDIMENTO';

	// Botão e endereço de cada situação de destino. Quais aparecem vem da API (proximasSituacoes).
	var acoesPorSituacao = {
		EM_ATENDIMENTO: { texto: 'Iniciar atendimento', verbo: 'iniciar', sucesso: 'Atendimento iniciado com sucesso.' },
		CONCLUIDO: { texto: 'Concluir', verbo: 'concluir', sucesso: 'Atendimento concluído com sucesso.' }
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

	function criarBotao(texto, emergencia, acao) {
		var botao = document.createElement('button');
		botao.type = 'button';
		botao.className = 'botao botao-secundario botao-pequeno';
		botao.textContent = texto;
		botao.setAttribute('aria-label', texto + ': ' + emergencia.paciente.nome + ', chegada em '
			+ formatarDataHora(emergencia.dataHoraChegada));
		botao.addEventListener('click', acao);
		return botao;
	}

	function adicionarOpcao(campo, valor, texto) {
		var opcao = document.createElement('option');
		opcao.value = valor;
		opcao.textContent = texto;
		campo.appendChild(opcao);
	}

	function trocarOpcoes(campo, itens, texto) {
		while (campo.options.length > 1) {
			campo.remove(1);
		}
		itens.forEach(function (item) {
			adicionarOpcao(campo, item.id, texto(item));
		});
	}

	// Data e hora local no formato do campo datetime-local (AAAA-MM-DDTHH:MM).
	function agora() {
		var data = new Date();
		data.setMinutes(data.getMinutes() - data.getTimezoneOffset());
		return data.toISOString().slice(0, 16);
	}

	function formatarDataHora(dataHora) {
		return new Date(dataHora).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
	}

	function descreverProfissional(profissional) {
		return profissional.nome + ' (' + profissional.categoria.descricao
			+ (profissional.areaAtuacao ? ' · ' + profissional.areaAtuacao.descricao : '') + ')';
	}

	function criarEtiqueta(classe, texto) {
		var etiqueta = document.createElement('span');
		etiqueta.className = 'etiqueta etiqueta-' + classe;
		etiqueta.textContent = texto;
		return etiqueta;
	}

	// Uma cor por nível de prioridade; sem prioridade, o paciente ainda aguarda a triagem.
	function etiquetaDaPrioridade(prioridade) {
		return prioridade
			? criarEtiqueta('prioridade-' + prioridade.codigo.toLowerCase(), prioridade.descricao)
			: criarEtiqueta('sem-prioridade', 'Aguardando triagem');
	}

	function celulaComElemento(linha, elemento) {
		var celula = criarCelula(linha, '');
		celula.replaceChildren(elemento);
		return celula;
	}

	function criarAcoes(emergencia) {
		var acoes = document.createElement('div');
		acoes.className = 'acoes';
		if (emergencia.permiteDefinirPrioridade) {
			acoes.appendChild(criarBotao(emergencia.prioridade ? 'Reclassificar prioridade' : 'Definir prioridade',
				emergencia, function () {
					abrirAcao(emergencia, formPrioridade);
				}));
		}
		emergencia.proximasSituacoes.forEach(function (situacao) {
			var acao = acoesPorSituacao[situacao.codigo];
			// A máquina de estados permite iniciar, mas a API também exige a prioridade (permiteIniciar).
			if (situacao.codigo === 'EM_ATENDIMENTO' && !emergencia.permiteIniciar) {
				if (!emergencia.prioridade) {
					var dica = document.createElement('span');
					dica.className = 'sem-acao';
					dica.textContent = 'Defina a prioridade para iniciar';
					acoes.appendChild(dica);
				}
				return;
			}
			acoes.appendChild(criarBotao(acao.texto, emergencia, function () {
				mudarSituacao(emergencia, acao);
			}));
		});
		if (emergencia.permiteRegistrarObservacoes) {
			acoes.appendChild(criarBotao('Observações', emergencia, function () {
				abrirAcao(emergencia, formObservacoes);
			}));
		}
		if (acoes.childElementCount === 0) {
			var nenhuma = document.createElement('span');
			nenhuma.className = 'sem-acao';
			nenhuma.textContent = 'Nenhuma ação disponível';
			return nenhuma;
		}
		return acoes;
	}

	function linhaVazia(corpo, colunas, mensagem) {
		var linha = document.createElement('tr');
		criarCelula(linha, mensagem).colSpan = colunas;
		corpo.appendChild(linha);
	}

	function preencherFila(emergencias) {
		corpoFila.replaceChildren();
		if (emergencias.length === 0) {
			linhaVazia(corpoFila, 5, 'Nenhum paciente aguardando avaliação.');
			return;
		}
		emergencias.forEach(function (emergencia) {
			var linha = document.createElement('tr');
			celulaComElemento(linha, etiquetaDaPrioridade(emergencia.prioridade));
			criarCelula(linha, formatarDataHora(emergencia.dataHoraChegada));
			criarCelula(linha, emergencia.paciente.nome, 'quebra');
			criarCelula(linha, emergencia.profissional.nome, 'quebra');
			celulaComElemento(linha, criarAcoes(emergencia));
			corpoFila.appendChild(linha);
		});
	}

	function preencherSituacao(emergencias) {
		corpoSituacao.replaceChildren();
		if (emergencias.length === 0) {
			linhaVazia(corpoSituacao, 7, 'Nenhum atendimento nesta situação.');
			return;
		}
		emergencias.forEach(function (emergencia) {
			var linha = document.createElement('tr');
			criarCelula(linha, formatarDataHora(emergencia.dataHoraChegada));
			criarCelula(linha, emergencia.paciente.nome, 'quebra');
			criarCelula(linha, emergencia.profissional.nome, 'quebra');
			celulaComElemento(linha, etiquetaDaPrioridade(emergencia.prioridade));
			celulaComElemento(linha, criarEtiqueta(emergencia.situacao.codigo.toLowerCase().replace(/_/g, '-'),
				emergencia.situacao.descricao));
			criarCelula(linha, emergencia.observacoes, 'texto-longo');
			celulaComElemento(linha, criarAcoes(emergencia));
			corpoSituacao.appendChild(linha);
		});
	}

	async function carregarFila() {
		try {
			preencherFila(await requisitar('/emergencias/fila'));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar a fila de avaliação. ' + erro.message);
		}
	}

	async function carregarPorSituacao() {
		if (!filtro.elements.situacao.value) {
			return;
		}
		try {
			preencherSituacao(await requisitar('/emergencias?situacao=' + filtro.elements.situacao.value));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os atendimentos. ' + erro.message);
		}
	}

	function recarregar() {
		return Promise.all([carregarFila(), carregarPorSituacao()]);
	}

	async function mudarSituacao(emergencia, acao) {
		try {
			await requisitar('/emergencias/' + emergencia.id + '/' + acao.verbo, { method: 'PATCH' });
			mostrarAviso(acao.sucesso);
			fecharAcao();
			await recarregar();
		} catch (erro) {
			mostrarAviso('Não foi possível alterar a situação do atendimento. ' + erro.message);
		}
	}

	// Prioridade e observações usam o mesmo painel, com o formulário de cada ação.
	function abrirAcao(emergencia, formulario) {
		emergenciaEmAcao = emergencia;
		formPrioridade.hidden = formulario !== formPrioridade;
		formObservacoes.hidden = formulario !== formObservacoes;
		if (formulario === formPrioridade) {
			document.getElementById('titulo-acao').textContent = emergencia.prioridade
				? 'Reclassificar prioridade' : 'Definir prioridade';
			formPrioridade.elements.prioridade.value = emergencia.prioridade ? emergencia.prioridade.codigo : '';
		} else {
			document.getElementById('titulo-acao').textContent = 'Registrar observações';
			formObservacoes.elements.texto.value = emergencia.observacoes || '';
		}
		document.getElementById('resumo-acao').textContent = 'Atendimento de ' + emergencia.paciente.nome
			+ ', chegada em ' + formatarDataHora(emergencia.dataHoraChegada) + '.';
		painelAcao.hidden = false;
		painelAcao.scrollIntoView({ behavior: 'smooth' });
		formulario.elements[0].focus();
	}

	function fecharAcao() {
		emergenciaEmAcao = null;
		painelAcao.hidden = true;
	}

	async function registrarAcao(evento, verbo, corpoRequisicao, sucesso) {
		evento.preventDefault();
		try {
			await requisitar('/emergencias/' + emergenciaEmAcao.id + '/' + verbo, {
				method: 'PATCH',
				body: JSON.stringify(corpoRequisicao)
			});
			mostrarAviso(sucesso);
			fecharAcao();
			await recarregar();
		} catch (erro) {
			mostrarAviso('Não foi possível concluir a ação. ' + erro.message);
		}
	}

	formPrioridade.addEventListener('submit', function (evento) {
		registrarAcao(evento, 'definir-prioridade', { prioridade: formPrioridade.elements.prioridade.value },
			'Prioridade registrada com sucesso.');
	});
	formObservacoes.addEventListener('submit', function (evento) {
		registrarAcao(evento, 'registrar-observacoes', { texto: formObservacoes.elements.texto.value },
			'Observações registradas com sucesso.');
	});
	painelAcao.querySelectorAll('[data-fechar]').forEach(function (botao) {
		botao.addEventListener('click', fecharAcao);
	});

	filtro.addEventListener('submit', function (evento) {
		evento.preventDefault();
		carregarPorSituacao();
	});

	formChegada.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var dados = new FormData(formChegada);
		try {
			await requisitar('/emergencias', {
				method: 'POST',
				body: JSON.stringify({
					pacienteId: Number(dados.get('pacienteId')),
					profissionalId: Number(dados.get('profissionalId')),
					prioridade: dados.get('prioridade') || null,
					dataHoraChegada: dados.get('dataHoraChegada')
				})
			});
			formChegada.reset();
			mostrarAviso('Chegada registrada com sucesso.');
			await recarregar();
		} catch (erro) {
			mostrarAviso('Não foi possível registrar a chegada. ' + erro.message);
		}
	});

	formChegada.addEventListener('reset', function () {
		// Os campos só voltam ao valor inicial depois deste evento.
		window.setTimeout(preencherAgora);
	});

	function preencherAgora() {
		formChegada.elements.dataHoraChegada.value = agora();
	}

	async function iniciar() {
		preencherAgora();
		try {
			var dominios = await Dominios.carregar();
			Dominios.preencher(formChegada.elements.prioridade, dominios.prioridadesEmergencia);
			Dominios.preencher(formPrioridade.elements.prioridade, dominios.prioridadesEmergencia);
			Dominios.preencher(filtro.elements.situacao, dominios.situacoesEmergencia);
			filtro.elements.situacao.value = SITUACAO_INICIAL_DA_LISTA;
			var listas = await Promise.all([requisitar('/pacientes?ativos=true'),
				requisitar('/profissionais?ativos=true')]);
			trocarOpcoes(formChegada.elements.pacienteId, listas[0], function (paciente) {
				return paciente.nome;
			});
			trocarOpcoes(formChegada.elements.profissionalId, listas[1], descreverProfissional);
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as opções da tela. ' + erro.message);
		}
		await recarregar();
	}

	iniciar();
})();
