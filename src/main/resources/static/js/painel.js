(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var aviso = document.getElementById('aviso-api');
	var LIMITE_DAS_LISTAS = 5;
	var temporizadorAviso;

	function mostrarAviso(mensagem) {
		aviso.textContent = mensagem;
		aviso.classList.add('visivel');
		window.clearTimeout(temporizadorAviso);
		temporizadorAviso = window.setTimeout(function () {
			aviso.classList.remove('visivel');
		}, 6000);
	}

	function formatarHorario(horario) {
		return horario.slice(0, 5);
	}

	function formatarDataHora(dataHora) {
		return new Date(dataHora).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
	}

	function mostrarDestaque(nome, valor, dica) {
		document.getElementById('valor-' + nome).textContent = valor;
		document.getElementById('dica-' + nome).textContent = dica;
	}

	// Quantos itens há em cada situação, na ordem em que aparecem: "Agendada: 2, Concluída: 1".
	function resumirPorSituacao(itens) {
		var contagem = new Map();
		itens.forEach(function (item) {
			contagem.set(item.situacao.descricao, (contagem.get(item.situacao.descricao) || 0) + 1);
		});
		return Array.from(contagem, function (par) {
			return par[0] + ': ' + par[1];
		}).join(', ') + '.';
	}

	// Ainda não encerrado: a API informa alguma situação seguinte possível.
	function emAberto(atendimento) {
		return atendimento.proximasSituacoes.length > 0;
	}

	function criarEtiqueta(classe, texto) {
		var etiqueta = document.createElement('span');
		etiqueta.className = 'etiqueta etiqueta-' + classe;
		etiqueta.textContent = texto;
		return etiqueta;
	}

	function etiquetaDaSituacao(situacao) {
		return criarEtiqueta(situacao.codigo.toLowerCase().replace(/_/g, '-'), situacao.descricao);
	}

	// Cada coluna é um texto ou um elemento (como uma etiqueta).
	function preencherTabela(id, itens, colunas, mensagemVazia) {
		var corpo = document.getElementById(id);
		corpo.replaceChildren();
		if (itens.length === 0) {
			var vazia = document.createElement('tr');
			var mensagem = document.createElement('td');
			mensagem.colSpan = corpo.closest('table').tHead.rows[0].cells.length;
			mensagem.textContent = mensagemVazia;
			vazia.appendChild(mensagem);
			corpo.appendChild(vazia);
			return;
		}
		itens.slice(0, LIMITE_DAS_LISTAS).forEach(function (item) {
			var linha = document.createElement('tr');
			colunas.forEach(function (coluna) {
				var celula = document.createElement('td');
				var conteudo = coluna.valor(item);
				if (conteudo instanceof Node) {
					celula.appendChild(conteudo);
				} else {
					celula.textContent = conteudo || '—';
				}
				if (coluna.quebra) {
					celula.className = 'quebra';
				}
				linha.appendChild(celula);
			});
			corpo.appendChild(linha);
		});
	}

	function mostrarConsultas(consultas) {
		mostrarDestaque('consultas', consultas.length,
			consultas.length ? resumirPorSituacao(consultas) : 'Nenhuma consulta marcada para hoje.');
		preencherTabela('lista-consultas', consultas.filter(emAberto), [
			{ valor: function (consulta) { return formatarHorario(consulta.horario); } },
			{ valor: function (consulta) { return consulta.paciente.nome; }, quebra: true },
			{ valor: function (consulta) { return consulta.profissional.nome; }, quebra: true },
			{ valor: function (consulta) { return consulta.motivo; }, quebra: true },
			{ valor: function (consulta) { return etiquetaDaSituacao(consulta.situacao); } }
		], 'Nenhuma consulta por atender hoje.');
	}

	function mostrarInternacoes(internacoes) {
		var hoje = DataAtual.hojeIso();
		var altasPrevistas = internacoes.filter(function (internacao) {
			return internacao.dataPrevistaAlta <= hoje;
		}).length;
		var dica = 'Nenhum paciente internado.';
		if (internacoes.length) {
			dica = altasPrevistas ? altasPrevistas + ' com alta prevista até hoje.' : 'Nenhuma alta prevista até hoje.';
		}
		mostrarDestaque('internacoes', internacoes.length, dica);
	}

	function mostrarQuartos(quartos) {
		var disponiveis = quartos.filter(function (quarto) {
			return quarto.situacao === 'DISPONIVEL';
		}).length;
		mostrarDestaque('quartos', disponiveis, descreverTotalDeQuartos(quartos.length));
	}

	function descreverTotalDeQuartos(total) {
		if (total === 0) {
			return 'Nenhum quarto cadastrado.';
		}
		return total === 1 ? 'De 1 quarto cadastrado.' : 'De ' + total + ' quartos cadastrados.';
	}

	function mostrarFila(fila) {
		var semPrioridade = fila.filter(function (emergencia) {
			return !emergencia.prioridade;
		}).length;
		var dica = 'Ninguém aguarda avaliação.';
		if (fila.length) {
			dica = semPrioridade ? semPrioridade + ' sem prioridade definida.' : 'Todos com prioridade definida.';
		}
		mostrarDestaque('fila', fila.length, dica);
		preencherTabela('lista-fila', fila, [
			{ valor: function (emergencia) {
				return emergencia.prioridade
					? criarEtiqueta('prioridade-' + emergencia.prioridade.codigo.toLowerCase(),
						emergencia.prioridade.descricao)
					: criarEtiqueta('sem-prioridade', 'Aguardando triagem');
			} },
			{ valor: function (emergencia) { return formatarDataHora(emergencia.dataHoraChegada); } },
			{ valor: function (emergencia) { return emergencia.paciente.nome; }, quebra: true },
			{ valor: function (emergencia) { return emergencia.profissional.nome; }, quebra: true }
		], 'Ninguém aguarda avaliação.');
	}

	function mostrarProcedimentos(procedimentos) {
		mostrarDestaque('procedimentos', procedimentos.length,
			procedimentos.length ? resumirPorSituacao(procedimentos) : 'Nenhum procedimento previsto para hoje.');
		preencherTabela('lista-procedimentos', procedimentos.filter(emAberto), [
			{ valor: function (procedimento) {
				return procedimento.dataHoraPrevista.slice(11, 16) + ' às ' + procedimento.dataHoraFimPrevista.slice(11, 16);
			} },
			{ valor: function (procedimento) { return procedimento.paciente.nome; }, quebra: true },
			{ valor: function (procedimento) { return procedimento.profissional.nome; }, quebra: true },
			{ valor: function (procedimento) { return procedimento.salaCirurgica.identificacao; } },
			{ valor: function (procedimento) { return procedimento.descricao; }, quebra: true },
			{ valor: function (procedimento) { return etiquetaDaSituacao(procedimento.situacao); } }
		], 'Nenhum procedimento por realizar hoje.');
	}

	// Cada bloco é carregado à parte: a falha de um não impede os outros de aparecer.
	var blocos = [
		{ nome: 'consultas', descricao: 'as consultas de hoje', endereco: '/consultas?data=', porData: true,
			mostrar: mostrarConsultas },
		{ nome: 'internacoes', descricao: 'as internações', endereco: '/internacoes?ativas=true',
			mostrar: mostrarInternacoes },
		{ nome: 'quartos', descricao: 'os quartos', endereco: '/quartos', mostrar: mostrarQuartos },
		{ nome: 'fila', descricao: 'a fila de emergência', endereco: '/emergencias/fila', mostrar: mostrarFila },
		{ nome: 'procedimentos', descricao: 'os procedimentos de hoje', endereco: '/procedimentos?data=',
			porData: true, mostrar: mostrarProcedimentos }
	];

	blocos.forEach(async function (bloco) {
		try {
			bloco.mostrar(await requisitar(bloco.endereco + (bloco.porData ? DataAtual.hojeIso() : '')));
		} catch (erro) {
			mostrarDestaque(bloco.nome, '—', 'Não foi possível carregar.');
			mostrarAviso('Não foi possível carregar ' + bloco.descricao + '. ' + erro.message);
		}
	});
})();
