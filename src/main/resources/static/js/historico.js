(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var formBusca = document.getElementById('form-busca');
	var resultadoBusca = document.getElementById('resultado-busca');
	var corpoPacientes = document.getElementById('lista-pacientes');
	var filtro = document.getElementById('form-filtro');
	var campoModalidade = filtro.elements.modalidade;
	var campoSituacao = filtro.elements.situacao;
	var linhaTempo = document.getElementById('linha-tempo');
	var aviso = document.getElementById('aviso-api');
	var dominios = null;
	var pacientes = [];
	var pacienteAtual = null;
	var temporizadorAviso;

	// Lista de situações (no GET /dominios) de cada modalidade; as duas consultas têm a mesma máquina de estados.
	var situacoesPorModalidade = {
		CONSULTA_ROTINA: 'situacoesConsulta',
		CONSULTA_ESPECIALIZADA: 'situacoesConsulta',
		EMERGENCIA: 'situacoesEmergencia',
		PROCEDIMENTO_CIRURGICO: 'situacoesProcedimento',
		INTERNACAO: 'situacoesInternacao'
	};

	// Com um único paciente na tela, ordenar pelo nome do paciente não muda nada.
	var ORDENACAO_SEM_EFEITO = 'PACIENTE';

	function mostrarAviso(mensagem) {
		aviso.textContent = mensagem;
		aviso.classList.add('visivel');
		window.clearTimeout(temporizadorAviso);
		temporizadorAviso = window.setTimeout(function () {
			aviso.classList.remove('visivel');
		}, 6000);
	}

	function adicionarOpcao(campo, valor, texto) {
		var opcao = document.createElement('option');
		opcao.value = valor;
		opcao.textContent = texto;
		campo.appendChild(opcao);
	}

	function criarCelula(linha, valor) {
		var celula = document.createElement('td');
		celula.textContent = valor || '—';
		linha.appendChild(celula);
		return celula;
	}

	function criarEtiqueta(situacao) {
		var etiqueta = document.createElement('span');
		etiqueta.className = 'etiqueta etiqueta-' + situacao.codigo.toLowerCase().replace(/_/g, '-');
		etiqueta.textContent = situacao.descricao;
		return etiqueta;
	}

	function criarEtiquetaDoPaciente(paciente) {
		return criarEtiqueta(paciente.ativo
			? { codigo: 'ativo', descricao: 'Ativo' } : { codigo: 'inativo', descricao: 'Inativo' });
	}

	function normalizar(texto) {
		return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
	}

	function formatarData(data) {
		return data ? new Date(data + 'T00:00:00').toLocaleDateString('pt-BR') : '';
	}

	function formatarDataHora(dataHora) {
		return dataHora ? new Date(dataHora).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
			: '';
	}

	function formatarCpf(cpf) {
		return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
	}

	function calcularIdade(dataNascimento) {
		var nascimento = new Date(dataNascimento + 'T00:00:00');
		var agora = new Date();
		var idade = agora.getFullYear() - nascimento.getFullYear();
		if (agora.getMonth() < nascimento.getMonth()
				|| (agora.getMonth() === nascimento.getMonth() && agora.getDate() < nascimento.getDate())) {
			idade--;
		}
		return idade;
	}

	function descreverIdade(idade) {
		return idade + (idade === 1 ? ' ano' : ' anos');
	}

	function descreverEndereco(endereco) {
		if (!endereco) {
			return '';
		}
		var cidade = endereco.cidade + '/' + endereco.uf.toUpperCase();
		return [endereco.logradouro, endereco.numero, endereco.complemento, endereco.bairro, cidade,
			endereco.cep ? endereco.cep.replace(/(\d{5})(\d{3})/, '$1-$2') : null]
			.filter(Boolean)
			.join(', ');
	}

	// Busca entre todos os pacientes, ativos e inativos, pelo nome (sem diferenciar acentos) ou pelo CPF.
	function buscarPacientes(termo) {
		var nome = normalizar(termo.trim());
		var digitos = termo.replace(/\D/g, '');
		return pacientes.filter(function (paciente) {
			return normalizar(paciente.nome).includes(nome) || (digitos.length > 0 && paciente.cpf.includes(digitos));
		});
	}

	function mostrarResultado(encontrados) {
		corpoPacientes.replaceChildren();
		resultadoBusca.hidden = false;
		if (encontrados.length === 0) {
			var linhaVazia = document.createElement('tr');
			criarCelula(linhaVazia, 'Nenhum paciente encontrado.').colSpan = 4;
			corpoPacientes.appendChild(linhaVazia);
			return;
		}
		encontrados.forEach(function (paciente) {
			var linha = document.createElement('tr');
			if (!paciente.ativo) {
				linha.className = 'inativo';
			}
			criarCelula(linha, paciente.nome).className = 'quebra';
			criarCelula(linha, formatarCpf(paciente.cpf));
			criarCelula(linha, '').replaceChildren(criarEtiquetaDoPaciente(paciente));
			var celulaAcoes = criarCelula(linha, '');
			var botao = document.createElement('button');
			botao.type = 'button';
			botao.className = 'botao botao-secundario botao-pequeno';
			botao.textContent = 'Ver histórico';
			botao.setAttribute('aria-label', 'Ver histórico de ' + paciente.nome);
			botao.addEventListener('click', function () {
				abrirPaciente(paciente.id);
			});
			celulaAcoes.replaceChildren(botao);
			corpoPacientes.appendChild(linha);
		});
	}

	function preencherDado(id, valor) {
		document.getElementById(id).textContent = valor || 'Não informado';
	}

	function mostrarPaciente(paciente) {
		document.getElementById('titulo-paciente').textContent = paciente.nome;
		preencherDado('dado-cpf', formatarCpf(paciente.cpf));
		preencherDado('dado-nascimento', paciente.dataNascimento
			? formatarData(paciente.dataNascimento) + ' (' + descreverIdade(calcularIdade(paciente.dataNascimento)) + ')'
			: '');
		preencherDado('dado-telefone', paciente.telefone);
		document.getElementById('dado-situacao').replaceChildren(criarEtiquetaDoPaciente(paciente));
		preencherDado('dado-endereco', descreverEndereco(paciente.endereco));
		preencherDado('dado-email', paciente.email);
	}

	function semObservacoes(texto) {
		return texto || 'Nenhuma observação registrada.';
	}

	function descreverConsulta(consulta) {
		var dados = [
			['Data e horário', formatarData(consulta.data) + ', ' + consulta.horario.slice(0, 5)],
			['Profissional', consulta.profissional.nome]
		];
		if (consulta.areaAtuacao) {
			dados.push(['Área de atuação', consulta.areaAtuacao.descricao]);
		}
		if (consulta.areaEncaminhamento) {
			dados.push(['Encaminhamento', consulta.areaEncaminhamento.descricao]);
		}
		dados.push(['Observações', semObservacoes(consulta.observacoes)]);
		return { titulo: consulta.modalidade.descricao + ': ' + consulta.motivo, dados: dados };
	}

	function descreverInternacao(internacao) {
		return {
			titulo: 'Internação no quarto ' + internacao.quarto.numero + ' (' + internacao.quarto.tipo.descricao + ')',
			classe: 'evento-internacao',
			dados: [
				['Entrada', formatarData(internacao.dataEntrada)],
				['Alta prevista', formatarData(internacao.dataPrevistaAlta)],
				['Alta efetiva', internacao.dataEfetivaAlta ? formatarData(internacao.dataEfetivaAlta)
					: 'Paciente ainda internado'],
				['Responsável', internacao.profissional.nome],
				['Observações', semObservacoes(internacao.observacoes)]
			]
		};
	}

	function descreverEmergencia(emergencia) {
		return {
			titulo: 'Atendimento de emergência',
			classe: 'evento-emergencia',
			dados: [
				['Chegada', formatarDataHora(emergencia.dataHoraChegada)],
				['Prioridade', emergencia.prioridade ? emergencia.prioridade.descricao : 'Ainda não definida'],
				['Profissional de plantão', emergencia.profissional.nome],
				['Observações', semObservacoes(emergencia.observacoes)]
			]
		};
	}

	function descreverProcedimento(procedimento) {
		var dados = [
			['Tipo de cirurgia', procedimento.tipoCirurgia.descricao],
			['Sala cirúrgica', procedimento.salaCirurgica.identificacao],
			['Previsão', formatarDataHora(procedimento.dataHoraPrevista) + ' (' + procedimento.duracaoPrevistaMinutos
				+ ' minutos)']
		];
		if (procedimento.dataHoraInicio) {
			dados.push(['Início', formatarDataHora(procedimento.dataHoraInicio)]);
		}
		if (procedimento.dataHoraFim) {
			dados.push(['Fim', formatarDataHora(procedimento.dataHoraFim)]);
		}
		dados.push(['Cirurgião', procedimento.profissional.nome],
			['Observações pré-operatórias', semObservacoes(procedimento.observacoesPreOperatorias)],
			['Observações pós-operatórias', semObservacoes(procedimento.observacoesPosOperatorias)]);
		if (procedimento.observacoes) {
			dados.push(['Observações', procedimento.observacoes]);
		}
		return { titulo: 'Procedimento cirúrgico: ' + procedimento.descricao, classe: 'evento-procedimento',
			dados: dados };
	}

	// Cada modalidade tem os seus dados próprios em "detalhes".
	var descricaoPorModalidade = {
		CONSULTA_ROTINA: descreverConsulta,
		CONSULTA_ESPECIALIZADA: descreverConsulta,
		INTERNACAO: descreverInternacao,
		EMERGENCIA: descreverEmergencia,
		PROCEDIMENTO_CIRURGICO: descreverProcedimento
	};

	function criarEvento(atendimento, posicao) {
		var descricao = descricaoPorModalidade[atendimento.modalidade.codigo](atendimento.detalhes);
		var item = document.createElement('li');
		if (descricao.classe) {
			item.className = descricao.classe;
		}
		var evento = document.createElement('article');
		evento.className = 'evento';
		evento.setAttribute('aria-labelledby', 'evento-' + posicao);

		var cabecalho = document.createElement('div');
		cabecalho.className = 'evento-cabecalho';
		var titulo = document.createElement('h3');
		titulo.id = 'evento-' + posicao;
		titulo.textContent = descricao.titulo;
		cabecalho.appendChild(titulo);
		cabecalho.appendChild(criarEtiqueta(atendimento.situacao));
		evento.appendChild(cabecalho);

		var lista = document.createElement('dl');
		descricao.dados.forEach(function (dado) {
			var termo = document.createElement('dt');
			termo.textContent = dado[0];
			var valor = document.createElement('dd');
			valor.textContent = dado[1];
			lista.appendChild(termo);
			lista.appendChild(valor);
		});
		evento.appendChild(lista);
		item.appendChild(evento);
		return item;
	}

	function mostrarAtendimentos(atendimentos) {
		linhaTempo.replaceChildren();
		var ordem = filtro.elements.ordem.options[filtro.elements.ordem.selectedIndex];
		var resumo = document.getElementById('resumo-atendimentos');
		if (atendimentos.length === 0) {
			resumo.textContent = 'Nenhum atendimento encontrado.';
			return;
		}
		resumo.textContent = atendimentos.length + (atendimentos.length === 1 ? ' atendimento' : ' atendimentos')
			+ '. Ordenação: ' + ordem.textContent.charAt(0).toLowerCase() + ordem.textContent.slice(1) + '.';
		atendimentos.forEach(function (atendimento, indice) {
			linhaTempo.appendChild(criarEvento(atendimento, indice + 1));
		});
	}

	async function carregarAtendimentos() {
		var parametros = new URLSearchParams({ pacienteId: pacienteAtual });
		['modalidade', 'situacao', 'inicio', 'fim', 'ordem'].forEach(function (nome) {
			if (filtro.elements[nome].value) {
				parametros.set(nome, filtro.elements[nome].value);
			}
		});
		try {
			mostrarAtendimentos(await requisitar('/atendimentos?' + parametros));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os atendimentos. ' + erro.message);
		}
	}

	async function abrirPaciente(id) {
		try {
			mostrarPaciente(await requisitar('/pacientes/' + id));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar o paciente. ' + erro.message);
			return;
		}
		pacienteAtual = id;
		filtro.reset();
		atualizarSituacoes();
		['painel-paciente', 'painel-filtros', 'painel-atendimentos'].forEach(function (painel) {
			document.getElementById(painel).hidden = false;
		});
		await carregarAtendimentos();
		document.getElementById('painel-paciente').scrollIntoView({ behavior: 'smooth' });
	}

	// Sem modalidade escolhida, a lista reúne as situações de todas, sem repetir os códigos comuns a mais de uma.
	function atualizarSituacoes() {
		// Se o GET /dominios falhou, o aviso já foi mostrado e o filtro fica só com "Todas as situações".
		if (!dominios) {
			return;
		}
		var selecionada = campoSituacao.value;
		var listas = campoModalidade.value
			? [situacoesPorModalidade[campoModalidade.value]]
			: Object.values(situacoesPorModalidade);
		var codigos = new Set();
		while (campoSituacao.options.length > 1) {
			campoSituacao.remove(1);
		}
		new Set(listas).forEach(function (lista) {
			dominios[lista].forEach(function (situacao) {
				if (!codigos.has(situacao.codigo)) {
					codigos.add(situacao.codigo);
					adicionarOpcao(campoSituacao, situacao.codigo, situacao.descricao);
				}
			});
		});
		campoSituacao.value = codigos.has(selecionada) ? selecionada : '';
	}

	campoModalidade.addEventListener('change', atualizarSituacoes);
	filtro.addEventListener('submit', function (evento) {
		evento.preventDefault();
		carregarAtendimentos();
	});
	formBusca.addEventListener('submit', function (evento) {
		evento.preventDefault();
		var encontrados = buscarPacientes(formBusca.elements.busca.value);
		mostrarResultado(encontrados);
		if (encontrados.length === 1) {
			abrirPaciente(encontrados[0].id);
		}
	});

	async function iniciar() {
		try {
			var respostas = await Promise.all([Dominios.carregar(), requisitar('/pacientes')]);
			dominios = respostas[0];
			pacientes = respostas[1];
			Dominios.preencher(campoModalidade, dominios.modalidades);
			Dominios.preencher(filtro.elements.ordem, dominios.ordenacoes.filter(function (ordenacao) {
				return ordenacao.codigo !== ORDENACAO_SEM_EFEITO;
			}));
			atualizarSituacoes();
		} catch (erro) {
			mostrarAviso('Não foi possível carregar as opções da tela. ' + erro.message);
		}
	}

	iniciar();
})();
