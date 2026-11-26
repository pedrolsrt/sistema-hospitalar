(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var filtro = document.getElementById('form-filtro');
	var corpo = document.getElementById('lista-consultas');
	var aviso = document.getElementById('aviso-api');
	var agendamento = document.getElementById('form-agendamento');
	var campoModalidade = agendamento.elements.modalidade;
	var campoArea = agendamento.elements.areaAtuacao;
	var campoProfissional = agendamento.elements.profissionalId;
	var painelAcao = document.getElementById('acao-consulta');
	var formObservacoes = document.getElementById('form-observacoes');
	var formEncaminhamento = document.getElementById('form-encaminhamento');
	var consultaEmAcao = null;
	var temporizadorAviso;

	// Endereço de agendamento de cada modalidade de consulta; a especializada exige a área.
	var agendamentoPorModalidade = {
		CONSULTA_ROTINA: '/consultas/rotina',
		CONSULTA_ESPECIALIZADA: '/consultas/especializadas'
	};

	// Botão e endereço de cada situação de destino. Quais aparecem vem da API (proximasSituacoes).
	var acoesPorSituacao = {
		CONFIRMADA: { texto: 'Confirmar', verbo: 'confirmar', estilo: 'botao-secundario',
			sucesso: 'Consulta confirmada com sucesso.' },
		EM_ATENDIMENTO: { texto: 'Iniciar atendimento', verbo: 'iniciar', estilo: 'botao-secundario',
			sucesso: 'Atendimento iniciado com sucesso.' },
		CONCLUIDA: { texto: 'Concluir', verbo: 'concluir', estilo: 'botao-secundario',
			sucesso: 'Consulta concluída com sucesso.' },
		CANCELADA: { texto: 'Cancelar', verbo: 'cancelar', estilo: 'botao-perigo',
			pergunta: 'Cancelar a consulta de ', sucesso: 'Consulta cancelada com sucesso.' },
		NAO_COMPARECEU: { texto: 'Não compareceu', verbo: 'registrar-nao-comparecimento', estilo: 'botao-perigo',
			pergunta: 'Registrar que o paciente não compareceu à consulta de ',
			sucesso: 'Não comparecimento registrado com sucesso.' }
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

	function criarBotao(texto, estilo, consulta, acao) {
		var botao = document.createElement('button');
		botao.type = 'button';
		botao.className = 'botao ' + estilo + ' botao-pequeno';
		botao.textContent = texto;
		botao.setAttribute('aria-label', texto + ': consulta de ' + consulta.paciente.nome + ' em '
			+ formatarData(consulta.data) + ' às ' + formatarHorario(consulta.horario));
		botao.addEventListener('click', acao);
		return botao;
	}

	function adicionarOpcao(campo, valor, texto) {
		var opcao = document.createElement('option');
		opcao.value = valor;
		opcao.textContent = texto;
		campo.appendChild(opcao);
	}

	// Mantém só a primeira opção ("Selecione", "Todos") e acrescenta as novas.
	function trocarOpcoes(campo, itens, texto) {
		while (campo.options.length > 1) {
			campo.remove(1);
		}
		itens.forEach(function (item) {
			adicionarOpcao(campo, item.id, texto(item));
		});
	}

	function formatarData(data) {
		return new Date(data + 'T00:00:00').toLocaleDateString('pt-BR');
	}

	function formatarHorario(horario) {
		return horario.slice(0, 5);
	}

	function descreverProfissional(profissional) {
		return profissional.nome + ' (' + profissional.categoria.descricao
			+ (profissional.areaAtuacao ? ' · ' + profissional.areaAtuacao.descricao : '') + ')';
	}

	function descreverConsulta(consulta) {
		return consulta.paciente.nome + ' com ' + consulta.profissional.nome + ' em ' + formatarData(consulta.data)
			+ ' às ' + formatarHorario(consulta.horario);
	}

	function preencherTabela(consultas) {
		corpo.replaceChildren();
		if (consultas.length === 0) {
			var linhaVazia = document.createElement('tr');
			var mensagem = criarCelula(linhaVazia, 'Nenhuma consulta encontrada.');
			mensagem.colSpan = 7;
			corpo.appendChild(linhaVazia);
			return;
		}

		consultas.forEach(function (consulta) {
			var linha = document.createElement('tr');
			criarCelula(linha, formatarData(consulta.data) + ' ' + formatarHorario(consulta.horario));
			criarCelula(linha, consulta.paciente.nome, 'quebra');
			criarCelula(linha, consulta.profissional.nome, 'quebra');

			var modalidade = criarCelula(linha, consulta.modalidade.descricao
				+ (consulta.areaAtuacao ? ' · ' + consulta.areaAtuacao.descricao : ''), 'quebra');
			if (consulta.areaEncaminhamento) {
				modalidade.appendChild(document.createElement('br'));
				var encaminhamento = document.createElement('span');
				encaminhamento.className = 'dica';
				encaminhamento.textContent = 'Encaminhado para ' + consulta.areaEncaminhamento.descricao;
				modalidade.appendChild(encaminhamento);
			}
			criarCelula(linha, consulta.motivo, 'quebra');

			var celulaSituacao = criarCelula(linha, '');
			celulaSituacao.replaceChildren();
			var etiqueta = document.createElement('span');
			etiqueta.className = 'etiqueta etiqueta-' + consulta.situacao.codigo.toLowerCase().replace(/_/g, '-');
			etiqueta.textContent = consulta.situacao.descricao;
			celulaSituacao.appendChild(etiqueta);

			preencherAcoes(criarCelula(linha, ''), consulta);
			corpo.appendChild(linha);
		});
	}

	function preencherAcoes(celula, consulta) {
		celula.replaceChildren();
		var acoes = document.createElement('div');
		acoes.className = 'acoes';
		consulta.proximasSituacoes.forEach(function (situacao) {
			var acao = acoesPorSituacao[situacao.codigo];
			acoes.appendChild(criarBotao(acao.texto, acao.estilo, consulta, function () {
				mudarSituacao(consulta, acao);
			}));
		});
		if (consulta.permiteRegistrarObservacoes) {
			acoes.appendChild(criarBotao('Observações', 'botao-secundario', consulta, function () {
				abrirAcao(consulta, formObservacoes);
			}));
		}
		if (consulta.permiteEncaminhar) {
			acoes.appendChild(criarBotao('Encaminhar', 'botao-secundario', consulta, function () {
				abrirAcao(consulta, formEncaminhamento);
			}));
		}
		if (acoes.childElementCount === 0) {
			var nenhuma = document.createElement('span');
			nenhuma.className = 'sem-acao';
			nenhuma.textContent = 'Nenhuma ação disponível';
			celula.appendChild(nenhuma);
			return;
		}
		celula.appendChild(acoes);
	}

	async function carregarConsultas() {
		var parametros = new URLSearchParams();
		['profissionalId', 'data', 'situacao'].forEach(function (nome) {
			if (filtro.elements[nome].value) {
				parametros.set(nome, filtro.elements[nome].value);
			}
		});
		corpo.replaceChildren();
		try {
			preencherTabela(await requisitar('/consultas?' + parametros));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as consultas. ' + erro.message);
		}
	}

	async function mudarSituacao(consulta, acao) {
		if (acao.pergunta && !window.confirm(acao.pergunta + descreverConsulta(consulta) + '?')) {
			return;
		}
		try {
			await requisitar('/consultas/' + consulta.id + '/' + acao.verbo, { method: 'PATCH' });
			mostrarAviso(acao.sucesso);
			fecharAcao();
			await carregarConsultas();
		} catch (erro) {
			mostrarAviso('Não foi possível alterar a situação da consulta. ' + erro.message);
		}
	}

	// Observações e encaminhamento usam o mesmo painel, com o formulário de cada ação.
	function abrirAcao(consulta, formulario) {
		consultaEmAcao = consulta;
		formObservacoes.hidden = formulario !== formObservacoes;
		formEncaminhamento.hidden = formulario !== formEncaminhamento;
		if (formulario === formObservacoes) {
			document.getElementById('titulo-acao').textContent = 'Registrar observações';
			formObservacoes.elements.texto.value = consulta.observacoes || '';
		} else {
			document.getElementById('titulo-acao').textContent = 'Encaminhar paciente';
			formEncaminhamento.elements.areaAtuacao.value = consulta.areaEncaminhamento
				? consulta.areaEncaminhamento.codigo : '';
		}
		document.getElementById('resumo-acao').textContent = 'Consulta de ' + descreverConsulta(consulta) + '.';
		painelAcao.hidden = false;
		painelAcao.scrollIntoView({ behavior: 'smooth' });
		formulario.elements[0].focus();
	}

	function fecharAcao() {
		consultaEmAcao = null;
		painelAcao.hidden = true;
	}

	async function registrarAcao(evento, verbo, corpoRequisicao, sucesso) {
		evento.preventDefault();
		try {
			await requisitar('/consultas/' + consultaEmAcao.id + '/' + verbo, {
				method: 'PATCH',
				body: JSON.stringify(corpoRequisicao)
			});
			mostrarAviso(sucesso);
			fecharAcao();
			await carregarConsultas();
		} catch (erro) {
			mostrarAviso('Não foi possível concluir a ação. ' + erro.message);
		}
	}

	formObservacoes.addEventListener('submit', function (evento) {
		registrarAcao(evento, 'registrar-observacoes', { texto: formObservacoes.elements.texto.value },
			'Observações registradas com sucesso.');
	});
	formEncaminhamento.addEventListener('submit', function (evento) {
		registrarAcao(evento, 'encaminhar', { areaAtuacao: formEncaminhamento.elements.areaAtuacao.value },
			'Encaminhamento registrado com sucesso.');
	});
	painelAcao.querySelectorAll('[data-fechar]').forEach(function (botao) {
		botao.addEventListener('click', fecharAcao);
	});

	function exigeArea() {
		return campoModalidade.value === 'CONSULTA_ESPECIALIZADA';
	}

	// Na especializada, só os profissionais ativos da área escolhida, pelo filtro da API.
	async function carregarProfissionaisDoAgendamento() {
		var parametros = new URLSearchParams({ ativos: 'true' });
		if (exigeArea()) {
			if (!campoArea.value) {
				trocarOpcoes(campoProfissional, [], descreverProfissional);
				return;
			}
			parametros.set('area', campoArea.value);
		}
		try {
			trocarOpcoes(campoProfissional, await requisitar('/profissionais?' + parametros), descreverProfissional);
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os profissionais. ' + erro.message);
		}
	}

	function atualizarModalidade() {
		document.getElementById('campo-area').style.display = exigeArea() ? '' : 'none';
		campoArea.required = exigeArea();
		if (!exigeArea()) {
			campoArea.value = '';
		}
		carregarProfissionaisDoAgendamento();
	}

	campoModalidade.addEventListener('change', atualizarModalidade);
	campoArea.addEventListener('change', carregarProfissionaisDoAgendamento);
	filtro.addEventListener('submit', function (evento) {
		evento.preventDefault();
		carregarConsultas();
	});
	agendamento.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var dados = new FormData(agendamento);
		var consulta = {
			pacienteId: Number(dados.get('pacienteId')),
			profissionalId: Number(dados.get('profissionalId')),
			data: dados.get('data'),
			horario: dados.get('horario'),
			motivo: dados.get('motivo')
		};
		if (exigeArea()) {
			consulta.areaAtuacao = dados.get('areaAtuacao');
		}

		try {
			await requisitar(agendamentoPorModalidade[campoModalidade.value], {
				method: 'POST',
				body: JSON.stringify(consulta)
			});
			agendamento.reset();
			mostrarAviso('Consulta agendada com sucesso.');
			await carregarConsultas();
		} catch (erro) {
			mostrarAviso('Não foi possível agendar a consulta. ' + erro.message);
		}
	});
	agendamento.addEventListener('reset', function () {
		// Os campos só voltam ao valor inicial depois deste evento.
		window.setTimeout(atualizarModalidade);
	});

	async function iniciar() {
		var hoje = new Date();
		hoje.setMinutes(hoje.getMinutes() - hoje.getTimezoneOffset());
		agendamento.elements.data.min = hoje.toISOString().slice(0, 10);
		try {
			var dominios = await Dominios.carregar();
			Dominios.preencher(campoModalidade, dominios.modalidades.filter(function (modalidade) {
				return agendamentoPorModalidade[modalidade.codigo];
			}));
			Dominios.preencher(campoArea, dominios.areasAtuacao);
			Dominios.preencher(formEncaminhamento.elements.areaAtuacao, dominios.areasAtuacao);
			Dominios.preencher(filtro.elements.situacao, dominios.situacoesConsulta);
			var listas = await Promise.all([requisitar('/profissionais'), requisitar('/pacientes?ativos=true')]);
			trocarOpcoes(filtro.elements.profissionalId, listas[0], descreverProfissional);
			trocarOpcoes(agendamento.elements.pacienteId, listas[1], function (paciente) {
				return paciente.nome;
			});
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as opções da tela. ' + erro.message);
		}
		atualizarModalidade();
		await carregarConsultas();
	}

	iniciar();
})();
