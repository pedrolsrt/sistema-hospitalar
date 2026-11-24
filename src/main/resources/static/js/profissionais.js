(function () {
	'use strict';

	var formulario = document.getElementById('form-profissional');
	var corpo = document.getElementById('lista-profissionais');
	var aviso = document.getElementById('aviso-api');
	var campoCategoria = formulario.elements.categoria;
	var campoArea = formulario.elements.areaAtuacao;
	var idEmEdicao = null;
	var temporizadorAviso;
	var categoriasComArea = ['MEDICO_ESPECIALISTA', 'CIRURGIAO'];

	async function requisitar(url, opcoes) {
		var resposta;
		try {
			resposta = await fetch(url, Object.assign({
				headers: { 'Content-Type': 'application/json' }
			}, opcoes || {}));
		} catch (erro) {
			throw new Error('Não foi possível conectar à API. Verifique se o servidor está disponível.');
		}

		if (!resposta.ok) {
			var texto = await resposta.text();
			var problema;
			try {
				problema = texto ? JSON.parse(texto) : {};
			} catch (erroDeParse) {
				throw new Error('A API respondeu com erro (' + resposta.status + ').');
			}
			var mensagensCampos = Array.isArray(problema.campos)
				? problema.campos.map(function (campo) {
					return campo.campo + ': ' + campo.mensagem;
				})
				: [];
			throw new Error(mensagensCampos.length
				? mensagensCampos.join(' ')
				: problema.detail || problema.title || 'A API respondeu com erro (' + resposta.status + ').');
		}

		return resposta.status === 204 ? null : resposta.json();
	}

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

	function criarBotao(texto, estilo, nome, acao) {
		var botao = document.createElement('button');
		botao.type = 'button';
		botao.className = 'botao ' + estilo + ' botao-pequeno';
		botao.textContent = texto;
		botao.setAttribute('aria-label', texto + ' profissional ' + nome);
		botao.addEventListener('click', acao);
		return botao;
	}

	function preencherTabela(profissionais) {
		corpo.replaceChildren();
		if (profissionais.length === 0) {
			var linhaVazia = document.createElement('tr');
			var mensagem = criarCelula(linhaVazia, 'Nenhum profissional cadastrado.');
			mensagem.colSpan = 6;
			corpo.appendChild(linhaVazia);
			return;
		}

		profissionais.forEach(function (profissional) {
			var linha = document.createElement('tr');
			if (!profissional.ativo) {
				linha.className = 'inativo';
			}
			criarCelula(linha, profissional.nome, 'quebra');
			criarCelula(linha, profissional.registroProfissional);
			criarCelula(linha, profissional.areaAtuacao
				? profissional.areaAtuacao.descricao : profissional.categoria.descricao, 'quebra');

			var contato = criarCelula(linha, '', 'quebra');
			contato.replaceChildren();
			var telefone = document.createElement('span');
			telefone.textContent = profissional.telefone || '—';
			contato.appendChild(telefone);
			if (profissional.email) {
				contato.appendChild(document.createElement('br'));
				var email = document.createElement('span');
				email.className = 'dica';
				email.textContent = profissional.email;
				contato.appendChild(email);
			}

			var celulaSituacao = criarCelula(linha, '');
			celulaSituacao.replaceChildren();
			var etiqueta = document.createElement('span');
			etiqueta.className = 'etiqueta etiqueta-' + (profissional.ativo ? 'ativo' : 'inativo');
			etiqueta.textContent = profissional.ativo ? 'Ativo' : 'Inativo';
			celulaSituacao.appendChild(etiqueta);

			var celulaAcoes = criarCelula(linha, '');
			celulaAcoes.replaceChildren();
			var acoes = document.createElement('div');
			acoes.className = 'acoes';
			acoes.appendChild(criarBotao('Editar', 'botao-secundario', profissional.nome, function () {
				editar(profissional);
			}));
			acoes.appendChild(criarBotao(profissional.ativo ? 'Inativar' : 'Reativar',
				profissional.ativo ? 'botao-perigo' : 'botao-secundario', profissional.nome, function () {
					alterarSituacao(profissional);
				}));
			celulaAcoes.appendChild(acoes);
			corpo.appendChild(linha);
		});
	}

	async function carregarProfissionais() {
		corpo.replaceChildren();
		try {
			preencherTabela(await requisitar('/profissionais'));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os profissionais. ' + erro.message);
		}
	}

	function atualizarCampoArea() {
		var exigeArea = categoriasComArea.indexOf(campoCategoria.value) !== -1;
		document.getElementById('campo-area').style.display = exigeArea ? '' : 'none';
		campoArea.required = exigeArea;
		if (!exigeArea) {
			campoArea.value = '';
		}
	}

	function editar(profissional) {
		idEmEdicao = profissional.id;
		formulario.elements.nome.value = profissional.nome;
		formulario.elements.registroProfissional.value = profissional.registroProfissional;
		campoCategoria.value = profissional.categoria.codigo;
		campoArea.value = profissional.areaAtuacao ? profissional.areaAtuacao.codigo : '';
		formulario.elements.telefone.value = profissional.telefone || '';
		formulario.elements.email.value = profissional.email || '';
		atualizarCampoArea();
		formulario.elements.registroProfissional.disabled = true;
		campoCategoria.disabled = true;
		campoArea.disabled = true;
		document.getElementById('titulo-cadastro').textContent = 'Editar profissional';
		formulario.querySelector('[type="submit"]').textContent = 'Salvar alterações';
		document.getElementById('cadastro').scrollIntoView({ behavior: 'smooth' });
		formulario.elements.nome.focus();
	}

	async function alterarSituacao(profissional) {
		var acao = profissional.ativo ? 'inativar' : 'reativar';
		try {
			await requisitar('/profissionais/' + profissional.id + '/' + acao, { method: 'PATCH' });
			mostrarAviso('Profissional ' + (profissional.ativo ? 'inativado' : 'reativado') + ' com sucesso.');
			await carregarProfissionais();
		} catch (erro) {
			mostrarAviso('Não foi possível alterar a situação do profissional. ' + erro.message);
		}
	}

	campoCategoria.addEventListener('change', atualizarCampoArea);
	formulario.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var dados = new FormData(formulario);
		var profissional = {
			nome: dados.get('nome'),
			telefone: dados.get('telefone') || null,
			email: dados.get('email') || null
		};
		var estavaEditando = idEmEdicao !== null;
		if (!estavaEditando) {
			profissional.registroProfissional = dados.get('registroProfissional');
			profissional.categoria = dados.get('categoria');
			profissional.areaAtuacao = dados.get('areaAtuacao') || null;
		}

		try {
			await requisitar(estavaEditando ? '/profissionais/' + idEmEdicao : '/profissionais', {
				method: estavaEditando ? 'PUT' : 'POST',
				body: JSON.stringify(profissional)
			});
			formulario.reset();
			mostrarAviso(estavaEditando
				? 'Profissional atualizado com sucesso.' : 'Profissional cadastrado com sucesso.');
			await carregarProfissionais();
		} catch (erro) {
			mostrarAviso('Não foi possível salvar o profissional. ' + erro.message);
		}
	});

	formulario.addEventListener('reset', function () {
		window.setTimeout(function () {
			idEmEdicao = null;
			formulario.elements.registroProfissional.disabled = false;
			campoCategoria.disabled = false;
			campoArea.disabled = false;
			document.getElementById('titulo-cadastro').textContent = 'Cadastrar profissional';
			formulario.querySelector('[type="submit"]').textContent = 'Salvar profissional';
			atualizarCampoArea();
		});
	});

	atualizarCampoArea();
	carregarProfissionais();
})();
