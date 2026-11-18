package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.PessoaResumo;
import br.pucminas.hospital.dto.QuartoResumo;
import br.pucminas.hospital.dto.SalaCirurgicaResumo;
import br.pucminas.hospital.model.Pessoa;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.SalaCirurgica;

/**
 * Referências resumidas a pessoas, quartos e salas cirúrgicas dentro das respostas de atendimentos.
 */
final class Resumos {

	private Resumos() {
	}

	static PessoaResumo de(Pessoa pessoa) {
		return new PessoaResumo(pessoa.getId(), pessoa.getNome());
	}

	static QuartoResumo de(Quarto quarto) {
		return new QuartoResumo(quarto.getId(), quarto.getNumero(), CodigoDescricao.de(quarto.getTipo()));
	}

	static SalaCirurgicaResumo de(SalaCirurgica sala) {
		return new SalaCirurgicaResumo(sala.getId(), sala.getIdentificacao());
	}

}
