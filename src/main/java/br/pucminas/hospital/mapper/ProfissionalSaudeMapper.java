package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.ProfissionalSaudeAtualizacaoRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeResponse;
import br.pucminas.hospital.model.CategoriaProfissional;
import br.pucminas.hospital.model.ProfissionalSaude;
import org.springframework.stereotype.Component;

@Component
public class ProfissionalSaudeMapper {

	/**
	 * Cria o profissional da categoria pedida pela fábrica de {@link CategoriaProfissional}.
	 */
	public ProfissionalSaude paraEntidade(ProfissionalSaudeRequest request) {
		ProfissionalSaude profissional = request.categoria().criar(request.nome().strip(),
				request.registroProfissional().strip(), request.areaAtuacao());
		profissional.setTelefone(Textos.vazioComoNulo(request.telefone()));
		profissional.setEmail(Textos.vazioComoNulo(request.email()));
		return profissional;
	}

	public void atualizar(ProfissionalSaude profissional, ProfissionalSaudeAtualizacaoRequest request) {
		profissional.setNome(request.nome().strip());
		profissional.setTelefone(Textos.vazioComoNulo(request.telefone()));
		profissional.setEmail(Textos.vazioComoNulo(request.email()));
	}

	public ProfissionalSaudeResponse paraResponse(ProfissionalSaude profissional) {
		return new ProfissionalSaudeResponse(profissional.getId(), profissional.getNome(),
				profissional.getRegistroProfissional(), CodigoDescricao.de(profissional.getCategoria()),
				CodigoDescricao.de(profissional.getArea()), profissional.getTelefone(), profissional.getEmail(),
				profissional.isAtivo());
	}

}
