(function () {
	'use strict';

	var requisitar = Api.requisitar;
	var formulario = document.getElementById('form-paciente');
	var corpo = document.getElementById('lista-pacientes');
	var aviso = document.getElementById('aviso-api');
	var idEmEdicao = null;
	var temporizadorAviso;
	var camposEndereco = ['logradouro', 'numero', 'complemento', 'bairro', 'cidade', 'uf', 'cep'];

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
		botao.setAttribute('aria-label', texto + ' paciente ' + nome);
		botao.addEventListener('click', acao);
		return botao;
	}

	function formatarCpf(cpf) {
		var digitos = cpf.replace(/\D/g, '');
		return digitos.length === 11
			? digitos.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
			: cpf;
	}

	function formatarData(data) {
		return data ? new Date(data + 'T00:00:00').toLocaleDateString('pt-BR') : '—';
	}

	function preencherTabela(pacientes) {
		corpo.replaceChildren();
		if (pacientes.length === 0) {
			var linhaVazia = document.createElement('tr');
			var mensagem = criarCelula(linhaVazia, 'Nenhum paciente cadastrado.');
			mensagem.colSpan = 7;
			corpo.appendChild(linhaVazia);
			return;
		}

		pacientes.forEach(function (paciente) {
			var linha = document.createElement('tr');
			if (!paciente.ativo) {
				linha.className = 'inativo';
			}
			criarCelula(linha, paciente.nome, 'quebra');
			criarCelula(linha, formatarCpf(paciente.cpf));
			criarCelula(linha, formatarData(paciente.dataNascimento));
			criarCelula(linha, paciente.telefone);
			criarCelula(linha, paciente.endereco
				? paciente.endereco.cidade + '/' + paciente.endereco.uf : null, 'quebra');

			var celulaSituacao = criarCelula(linha, '');
			celulaSituacao.replaceChildren();
			var etiqueta = document.createElement('span');
			etiqueta.className = 'etiqueta etiqueta-' + (paciente.ativo ? 'ativo' : 'inativo');
			etiqueta.textContent = paciente.ativo ? 'Ativo' : 'Inativo';
			celulaSituacao.appendChild(etiqueta);

			var celulaAcoes = criarCelula(linha, '');
			celulaAcoes.replaceChildren();
			var acoes = document.createElement('div');
			acoes.className = 'acoes';
			acoes.appendChild(criarBotao('Editar', 'botao-secundario', paciente.nome, function () {
				editar(paciente);
			}));
			acoes.appendChild(criarBotao(paciente.ativo ? 'Inativar' : 'Reativar',
				paciente.ativo ? 'botao-perigo' : 'botao-secundario', paciente.nome, function () {
					alterarSituacao(paciente);
				}));
			celulaAcoes.appendChild(acoes);
			corpo.appendChild(linha);
		});
	}

	async function carregarPacientes() {
		corpo.replaceChildren();
		try {
			preencherTabela(await requisitar('/pacientes'));
		} catch (erro) {
			mostrarAviso('Não foi possível carregar os pacientes. ' + erro.message);
		}
	}

	function criarEndereco(dados) {
		var enderecoInformado = camposEndereco.some(function (campo) {
			return dados.get(campo);
		});
		if (!enderecoInformado) {
			return null;
		}
		return camposEndereco.reduce(function (endereco, campo) {
			endereco[campo] = dados.get(campo) || null;
			return endereco;
		}, {});
	}

	function editar(paciente) {
		idEmEdicao = paciente.id;
		formulario.elements.nome.value = paciente.nome;
		formulario.elements.cpf.value = formatarCpf(paciente.cpf);
		formulario.elements.cpf.disabled = true;
		formulario.elements.dataNascimento.value = paciente.dataNascimento;
		formulario.elements.telefone.value = paciente.telefone || '';
		formulario.elements.email.value = paciente.email || '';
		camposEndereco.forEach(function (campo) {
			formulario.elements[campo].value = paciente.endereco ? paciente.endereco[campo] || '' : '';
		});
		atualizarRequisitosEndereco();
		document.getElementById('titulo-cadastro').textContent = 'Editar paciente';
		formulario.querySelector('[type="submit"]').textContent = 'Salvar alterações';
		document.getElementById('cadastro').scrollIntoView({ behavior: 'smooth' });
		formulario.elements.nome.focus();
	}

	async function alterarSituacao(paciente) {
		var acao = paciente.ativo ? 'inativar' : 'reativar';
		try {
			await requisitar('/pacientes/' + paciente.id + '/' + acao, { method: 'PATCH' });
			mostrarAviso('Paciente ' + (paciente.ativo ? 'inativado' : 'reativado') + ' com sucesso.');
			await carregarPacientes();
		} catch (erro) {
			mostrarAviso('Não foi possível alterar a situação do paciente. ' + erro.message);
		}
	}

	function atualizarRequisitosEndereco() {
		var enderecoInformado = camposEndereco.some(function (campo) {
			return formulario.elements[campo].value.trim() !== '';
		});
		['logradouro', 'cidade', 'uf'].forEach(function (campo) {
			formulario.elements[campo].required = enderecoInformado;
		});
	}

	formulario.addEventListener('input', atualizarRequisitosEndereco);
	formulario.addEventListener('change', atualizarRequisitosEndereco);
	formulario.addEventListener('submit', async function (evento) {
		evento.preventDefault();
		var dados = new FormData(formulario);
		var paciente = {
			nome: dados.get('nome'),
			dataNascimento: dados.get('dataNascimento'),
			telefone: dados.get('telefone') || null,
			email: dados.get('email') || null,
			endereco: criarEndereco(dados)
		};
		var estavaEditando = idEmEdicao !== null;
		if (!estavaEditando) {
			paciente.cpf = dados.get('cpf');
		}

		try {
			await requisitar(estavaEditando ? '/pacientes/' + idEmEdicao : '/pacientes', {
				method: estavaEditando ? 'PUT' : 'POST',
				body: JSON.stringify(paciente)
			});
			formulario.reset();
			mostrarAviso(estavaEditando
				? 'Paciente atualizado com sucesso.' : 'Paciente cadastrado com sucesso.');
			await carregarPacientes();
		} catch (erro) {
			mostrarAviso('Não foi possível salvar o paciente. ' + erro.message);
		}
	});

	formulario.addEventListener('reset', function () {
		idEmEdicao = null;
		formulario.elements.cpf.disabled = false;
		document.getElementById('titulo-cadastro').textContent = 'Cadastrar paciente';
		formulario.querySelector('[type="submit"]').textContent = 'Salvar paciente';
		// Os campos só voltam ao valor inicial depois deste evento.
		window.setTimeout(atualizarRequisitosEndereco);
	});

	carregarPacientes();
})();
