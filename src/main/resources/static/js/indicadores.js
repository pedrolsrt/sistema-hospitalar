(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var filtro = document.getElementById('form-filtro');
	var aviso = document.getElementById('aviso-api');
	var temporizadorAviso;

	function mostrarAviso(mensagem) {
		aviso.textContent = mensagem;
		aviso.classList.add('visivel');
		window.clearTimeout(temporizadorAviso);
		temporizadorAviso = window.setTimeout(function () {
			aviso.classList.remove('visivel');
		}, 6000);
	}

	function formatarData(data) {
		return new Date(data + 'T00:00:00').toLocaleDateString('pt-BR');
	}

	function formatarNumero(numero) {
		return numero.toLocaleString('pt-BR');
	}

	function descreverPeriodo(inicio, fim) {
		if (inicio && inicio === fim) {
			return 'Em ' + formatarData(inicio) + '.';
		}
		if (inicio && fim) {
			return 'De ' + formatarData(inicio) + ' a ' + formatarData(fim) + '.';
		}
		if (inicio) {
			return 'A partir de ' + formatarData(inicio) + '.';
		}
		if (fim) {
			return 'Até ' + formatarData(fim) + '.';
		}
		return 'Todo o período.';
	}

	function descreverTotalDeQuartos(total) {
		if (total === 0) {
			return 'Nenhum quarto cadastrado.';
		}
		return (total === 1 ? 'De 1 quarto cadastrado' : 'De ' + formatarNumero(total) + ' quartos cadastrados')
			+ ', na situação atual.';
	}

	// A barra é só visual; o número fica ao lado, em texto, para leitores de tela.
	function criarBarra(quantidade, maximo) {
		var barra = document.createElement('div');
		barra.className = 'barra';
		barra.setAttribute('aria-hidden', 'true');
		var preenchida = document.createElement('div');
		preenchida.className = 'barra-preenchida';
		preenchida.style.width = (maximo > 0 ? quantidade / maximo * 100 : 0) + '%';
		barra.appendChild(preenchida);
		return barra;
	}

	// Sem referência, as barras são proporcionais ao maior valor da lista.
	function preencherBarras(lista, itens, referencia) {
		lista.replaceChildren();
		var maximo = referencia !== undefined ? referencia : Math.max.apply(null, itens.map(function (item) {
			return item.quantidade;
		}).concat(0));
		itens.forEach(function (item) {
			var linha = document.createElement('li');
			var rotulo = document.createElement('span');
			rotulo.className = 'barra-rotulo';
			rotulo.textContent = item.rotulo;
			var valor = document.createElement('span');
			valor.className = 'barra-valor';
			valor.textContent = formatarNumero(item.quantidade);
			linha.appendChild(rotulo);
			linha.appendChild(criarBarra(item.quantidade, maximo));
			linha.appendChild(valor);
			lista.appendChild(linha);
		});
	}

	function preencherProfissionais(profissionais) {
		var corpo = document.getElementById('lista-profissionais');
		corpo.replaceChildren();
		if (profissionais.length === 0) {
			var vazia = document.createElement('tr');
			var mensagem = document.createElement('td');
			mensagem.colSpan = 3;
			mensagem.textContent = 'Nenhum atendimento no período.';
			vazia.appendChild(mensagem);
			corpo.appendChild(vazia);
			return;
		}
		// A API já devolve do maior para o menor número; o primeiro é o maior.
		var maximo = profissionais[0].quantidade;
		profissionais.forEach(function (profissional) {
			var linha = document.createElement('tr');
			var nome = document.createElement('td');
			nome.className = 'quebra';
			nome.textContent = profissional.nome;
			var categoria = document.createElement('td');
			categoria.textContent = profissional.categoria.descricao;
			var quantidade = document.createElement('td');
			var conteudo = document.createElement('div');
			conteudo.className = 'barra-na-tabela';
			var valor = document.createElement('span');
			valor.className = 'barra-valor';
			valor.textContent = formatarNumero(profissional.quantidade);
			conteudo.appendChild(criarBarra(profissional.quantidade, maximo));
			conteudo.appendChild(valor);
			quantidade.appendChild(conteudo);
			linha.appendChild(nome);
			linha.appendChild(categoria);
			linha.appendChild(quantidade);
			corpo.appendChild(linha);
		});
	}

	function mostrarIndicadores(indicadores) {
		document.getElementById('descricao-periodo').textContent = descreverPeriodo(indicadores.inicio, indicadores.fim);
		document.getElementById('valor-consultas').textContent = formatarNumero(indicadores.consultasRealizadas);

		var total = indicadores.atendimentosPorModalidade.reduce(function (soma, item) {
			return soma + item.quantidade;
		}, 0);
		document.getElementById('valor-atendimentos').textContent = formatarNumero(total);
		preencherBarras(document.getElementById('barras-modalidade'),
			indicadores.atendimentosPorModalidade.map(function (item) {
				return { rotulo: item.modalidade.descricao, quantidade: item.quantidade };
			}));

		var quartos = indicadores.quartos;
		var totalQuartos = quartos.disponiveis + quartos.ocupados;
		document.getElementById('valor-quartos-disponiveis').textContent = formatarNumero(quartos.disponiveis);
		document.getElementById('dica-quartos').textContent = descreverTotalDeQuartos(totalQuartos);
		// As duas barras usam o total de quartos como referência, para mostrar a proporção de cada situação.
		preencherBarras(document.getElementById('barras-quartos'), [
			{ rotulo: 'Disponíveis', quantidade: quartos.disponiveis },
			{ rotulo: 'Ocupados', quantidade: quartos.ocupados }
		], totalQuartos);

		preencherProfissionais(indicadores.atendimentosPorProfissional);
	}

	async function carregar() {
		var parametros = new URLSearchParams();
		['inicio', 'fim'].forEach(function (nome) {
			if (filtro.elements[nome].value) {
				parametros.set(nome, filtro.elements[nome].value);
			}
		});
		try {
			mostrarIndicadores(await requisitar('/indicadores?' + parametros));
		} catch (erro) {
			mostrarAviso('Não foi possível calcular os indicadores. ' + erro.message);
		}
	}

	filtro.addEventListener('submit', function (evento) {
		evento.preventDefault();
		carregar();
	});
	filtro.addEventListener('reset', function () {
		// Os campos só ficam vazios depois deste evento.
		window.setTimeout(carregar);
	});

	carregar();
})();
