(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var filtro = document.getElementById('form-filtro');
	var corpo = document.getElementById('lista-procedimentos');
	var aviso = document.getElementById('aviso-api');
	var agendamento = document.getElementById('form-agendamento');
	var painelAcao = document.getElementById('acao-procedimento');
	var formDataHora = document.getElementById('form-data-hora');
	var formObservacoes = document.getElementById('form-observacoes');
	var procedimentoEmAcao = null;
	var acaoEmAndamento = null;
	var temporizadorAviso;

	// Só o cirurgião realiza procedimentos; a API recusa qualquer outra categoria.
	var CATEGORIA_CIRURGIAO = 'CIRURGIAO';

	// Ações de cada situação de destino. Quais aparecem vem da API (proximasSituacoes); iniciar e concluir
	// pedem a data e hora, cancelar pede confirmação.
	var acoesPorSituacao = {
		EM_ANDAMENTO: { texto: 'Iniciar', titulo: 'Iniciar procedimento', verbo: 'iniciar', campo: 'dataHoraInicio',
			rotulo: 'Data e hora de início', estilo: 'botao-secundario', sucesso: 'Procedimento iniciado com sucesso.' },
		CONCLUIDO: { texto: 'Concluir', titulo: 'Concluir procedimento', verbo: 'concluir', campo: 'dataHoraFim',
			rotulo: 'Data e hora de fim', estilo: 'botao-secundario', sucesso: 'Procedimento concluído com sucesso.' },
		CANCELADO: { texto: 'Cancelar', verbo: 'cancelar', estilo: 'botao-perigo', pergunta: 'Cancelar o procedimento ',
			sucesso: 'Procedimento cancelado com sucesso.' }
	};

	// Os dois registros de observações, liberados pela API (permiteRegistrarObservacoes...).
	var observacoes = {
		pre: { texto: 'Observações pré-operatórias', verbo: 'registrar-observacoes-pre-operatorias',
			permitido: 'permiteRegistrarObservacoesPreOperatorias', valor: 'observacoesPreOperatorias' },
		pos: { texto: 'Observações pós-operatórias', verbo: 'registrar-observacoes-pos-operatorias',
			permitido: 'permiteRegistrarObservacoesPosOperatorias', valor: 'observacoesPosOperatorias' }
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

	function criarBotao(texto, estilo, procedimento, acao) {
		var botao = document.createElement('button');
		botao.type = 'button';
		botao.className = 'botao ' + estilo + ' botao-pequeno';
		botao.textContent = texto;
		botao.setAttribute('aria-label', texto + ': ' + descreverProcedimento(procedimento));
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

	function hoje() {
		return agora().slice(0, 10);
	}

	// Data e hora local no formato do campo datetime-local (AAAA-MM-DDTHH:MM).
	function agora() {
		var data = new Date();
		data.setMinutes(data.getMinutes() - data.getTimezoneOffset());
		return data.toISOString().slice(0, 16);
	}

	function formatarHorario(dataHora) {
		return dataHora.slice(11, 16);
	}

	function formatarDataHora(dataHora) {
		return new Date(dataHora).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
	}

	function descreverProcedimento(procedimento) {
		return procedimento.descricao + ' de ' + procedimento.paciente.nome + ' em '
			+ formatarDataHora(procedimento.dataHoraPrevista);
	}

	// A lista é de um único dia: a data só aparece quando o início ou o fim cai em outro dia.
	function formatarNoDia(dataHora, dia) {
		return dataHora.slice(0, 10) === dia ? formatarHorario(dataHora) : formatarDataHora(dataHora);
	}

	function descreverRealizacao(procedimento) {
		if (!procedimento.dataHoraInicio) {
			return '';
		}
		var dia = procedimento.dataHoraPrevista.slice(0, 10);
		return 'Início ' + formatarNoDia(procedimento.dataHoraInicio, dia)
			+ (procedimento.dataHoraFim ? ', fim ' + formatarNoDia(procedimento.dataHoraFim, dia) : '');
	}

	function preencherTabela(procedimentos) {
		corpo.replaceChildren();
		if (procedimentos.length === 0) {
			var linhaVazia = document.createElement('tr');
			criarCelula(linhaVazia, 'Nenhum procedimento previsto para esta data.').colSpan = 8;
			corpo.appendChild(linhaVazia);
			return;
		}

		procedimentos.forEach(function (procedimento) {
			var linha = document.createElement('tr');
			criarCelula(linha, formatarHorario(procedimento.dataHoraPrevista) + ' às '
				+ formatarHorario(procedimento.dataHoraFimPrevista) + ' (' + procedimento.duracaoPrevistaMinutos
				+ ' min)');
			criarCelula(linha, procedimento.paciente.nome, 'quebra');
			criarCelula(linha, procedimento.profissional.nome, 'quebra');
			criarCelula(linha, procedimento.salaCirurgica.identificacao);

			var celulaProcedimento = criarCelula(linha, procedimento.descricao, 'quebra');
			celulaProcedimento.appendChild(document.createElement('br'));
			var tipo = document.createElement('span');
			tipo.className = 'dica';
			tipo.textContent = procedimento.tipoCirurgia.descricao;
			celulaProcedimento.appendChild(tipo);

			criarCelula(linha, descreverRealizacao(procedimento), 'quebra');

			var celulaSituacao = criarCelula(linha, '');
			var etiqueta = document.createElement('span');
			etiqueta.className = 'etiqueta etiqueta-' + procedimento.situacao.codigo.toLowerCase().replace(/_/g, '-');
			etiqueta.textContent = procedimento.situacao.descricao;
			celulaSituacao.replaceChildren(etiqueta);

			preencherAcoes(criarCelula(linha, ''), procedimento);
			corpo.appendChild(linha);
		});
	}

	function preencherAcoes(celula, procedimento) {
		var acoes = document.createElement('div');
		acoes.className = 'acoes';
		procedimento.proximasSituacoes.forEach(function (situacao) {
			var acao = acoesPorSituacao[situacao.codigo];
			acoes.appendChild(criarBotao(acao.texto, acao.estilo, procedimento, function () {
				if (acao.campo) {
					abrirDataHora(procedimento, acao);
				} else {
					mudarSituacao(procedimento, acao);
				}
			}));
		});
		Object.values(observacoes).forEach(function (registro) {
			if (procedimento[registro.permitido]) {
				acoes.appendChild(criarBotao(registro.texto, 'botao-secundario', procedimento, function () {
					abrirObservacoes(procedimento, registro);
				}));
			}
		});
		if (acoes.childElementCount === 0) {
			var nenhuma = document.createElement('span');
			nenhuma.className = 'sem-acao';
			nenhuma.textContent = 'Nenhuma ação disponível';
			celula.replaceChildren(nenhuma);
			return;
		}
		celula.replaceChildren(acoes);
	}

	async function carregarProcedimentos() {
		document.getElementById('titulo-lista').textContent = 'Procedimentos de '
			+ new Date(filtro.elements.data.value + 'T00:00:00').toLocaleDateString('pt-BR');
		corpo.replaceChildren();
		try {
			preencherTabela(await requisitar('/procedimentos?data=' + filtro.elements.data.value));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os procedimentos. ' + erro.message);
		}
	}

	async function mudarSituacao(procedimento, acao) {
		if (!window.confirm(acao.pergunta + descreverProcedimento(procedimento) + '?')) {
			return;
		}
		try {
			await requisitar('/procedimentos/' + procedimento.id + '/' + acao.verbo, { method: 'PATCH' });
			mostrarAviso(acao.sucesso);
			fecharAcao();
			await carregarProcedimentos();
		} catch (erro) {
			mostrarAviso('Não foi possível alterar a situação do procedimento. ' + erro.message);
		}
	}

	function abrirPainel(procedimento, formulario, titulo) {
		procedimentoEmAcao = procedimento;
		formDataHora.hidden = formulario !== formDataHora;
		formObservacoes.hidden = formulario !== formObservacoes;
		document.getElementById('titulo-acao').textContent = titulo;
		document.getElementById('resumo-acao').textContent = 'Procedimento: ' + descreverProcedimento(procedimento)
			+ ', na ' + procedimento.salaCirurgica.identificacao + '.';
		painelAcao.hidden = false;
		painelAcao.scrollIntoView({ behavior: 'smooth' });
		formulario.elements[0].focus();
	}

	// Iniciar e concluir usam o mesmo formulário de data e hora, com o rótulo de cada ação.
	function abrirDataHora(procedimento, acao) {
		acaoEmAndamento = acao;
		var campo = formDataHora.elements.dataHora;
		document.getElementById('rotulo-data-hora').firstChild.textContent = acao.rotulo + ' ';
		document.getElementById('confirmar-data-hora').textContent = acao.texto;
		campo.min = procedimento.dataHoraInicio ? procedimento.dataHoraInicio.slice(0, 16) : '';
		campo.value = agora();
		abrirPainel(procedimento, formDataHora, acao.titulo);
	}

	function abrirObservacoes(procedimento, registro) {
		acaoEmAndamento = registro;
		document.getElementById('rotulo-observacoes').firstChild.textContent = registro.texto + ' ';
		formObservacoes.elements.texto.value = procedimento[registro.valor] || '';
		abrirPainel(procedimento, formObservacoes, 'Registrar ' + registro.texto.toLowerCase());
	}

	function fecharAcao() {
		procedimentoEmAcao = null;
		acaoEmAndamento = null;
		painelAcao.hidden = true;
	}

	async function enviarAcao(evento, corpoRequisicao, sucesso) {
		evento.preventDefault();
		try {
			await requisitar('/procedimentos/' + procedimentoEmAcao.id + '/' + acaoEmAndamento.verbo, {
				method: 'PATCH',
				body: JSON.stringify(corpoRequisicao)
			});
			mostrarAviso(sucesso);
			fecharAcao();
			await carregarProcedimentos();
		} catch (erro) {
			mostrarAviso('Não foi possível concluir a ação. ' + erro.message);
		}
	}

	formDataHora.addEventListener('submit', function (evento) {
		var corpoRequisicao = {};
		corpoRequisicao[acaoEmAndamento.campo] = formDataHora.elements.dataHora.value;
		enviarAcao(evento, corpoRequisicao, acaoEmAndamento.sucesso);
	});
	formObservacoes.addEventListener('submit', function (evento) {
		enviarAcao(evento, { texto: formObservacoes.elements.texto.value }, 'Observações registradas com sucesso.');
	});
	painelAcao.querySelectorAll('[data-fechar]').forEach(function (botao) {
		botao.addEventListener('click', fecharAcao);
	});

	filtro.addEventListener('submit', function (evento) {
		evento.preventDefault();
		fecharAcao();
		carregarProcedimentos();
	});

	agendamento.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var dados = new FormData(agendamento);
		var observacoesPre = dados.get('observacoesPreOperatorias').trim();
		try {
			var agendado = await requisitar('/procedimentos', {
				method: 'POST',
				body: JSON.stringify({
					pacienteId: Number(dados.get('pacienteId')),
					cirurgiaoId: Number(dados.get('cirurgiaoId')),
					salaCirurgicaId: Number(dados.get('salaCirurgicaId')),
					tipoCirurgia: dados.get('tipoCirurgia'),
					descricao: dados.get('descricao'),
					dataHoraPrevista: dados.get('dataHoraPrevista'),
					duracaoPrevistaMinutos: Number(dados.get('duracaoPrevistaMinutos')),
					observacoesPreOperatorias: observacoesPre || null
				})
			});
			agendamento.reset();
			mostrarAviso('Procedimento agendado com sucesso.');
			// Mostra o dia do procedimento recém-agendado, onde ele aparece na lista.
			filtro.elements.data.value = agendado.dataHoraPrevista.slice(0, 10);
			await carregarProcedimentos();
		} catch (erro) {
			mostrarAviso('Não foi possível agendar o procedimento. ' + erro.message);
			// Uma recusa por sala inativa mostra que a lista de salas estava desatualizada.
			await carregarSalas();
		}
	});

	async function carregarSalas() {
		var campo = agendamento.elements.salaCirurgicaId;
		var selecionada = campo.value;
		try {
			trocarOpcoes(campo, await requisitar('/salas-cirurgicas?ativas=true'), function (sala) {
				return sala.identificacao;
			});
			campo.value = selecionada;
			if (campo.selectedIndex < 0) {
				campo.value = '';
			}
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as salas cirúrgicas. ' + erro.message);
		}
	}

	async function iniciar() {
		filtro.elements.data.value = hoje();
		try {
			var dominios = await Dominios.carregar();
			Dominios.preencher(agendamento.elements.tipoCirurgia, dominios.tiposCirurgia);
			var listas = await Promise.all([requisitar('/pacientes?ativos=true'),
				requisitar('/profissionais?ativos=true')]);
			trocarOpcoes(agendamento.elements.pacienteId, listas[0], function (paciente) {
				return paciente.nome;
			});
			trocarOpcoes(agendamento.elements.cirurgiaoId, listas[1].filter(function (profissional) {
				return profissional.categoria.codigo === CATEGORIA_CIRURGIAO;
			}), function (cirurgiao) {
				return cirurgiao.nome + (cirurgiao.areaAtuacao ? ' (' + cirurgiao.areaAtuacao.descricao + ')' : '');
			});
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as opções da tela. ' + erro.message);
		}
		await Promise.all([carregarSalas(), carregarProcedimentos()]);
	}

	iniciar();
})();
