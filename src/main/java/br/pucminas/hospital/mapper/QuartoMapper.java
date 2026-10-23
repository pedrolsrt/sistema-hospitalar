package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.QuartoRequest;
import br.pucminas.hospital.dto.QuartoResponse;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
import org.springframework.stereotype.Component;

@Component
public class QuartoMapper {

	public Quarto paraEntidade(QuartoRequest request) {
		return new Quarto(request.numero().strip(), request.andar(), request.tipo(), request.capacidadeMaxima());
	}

	public void atualizar(Quarto quarto, QuartoRequest request) {
		quarto.setNumero(request.numero().strip());
		quarto.setAndar(request.andar());
		quarto.alterarTipoECapacidade(request.tipo(), request.capacidadeMaxima());
	}

	public QuartoResponse paraResponse(Quarto quarto) {
		TipoQuarto tipo = quarto.getTipo();
		return new QuartoResponse(quarto.getId(), quarto.getNumero(), quarto.getAndar(),
				new CodigoDescricao(tipo.name(), tipo.getDescricao()), quarto.getCapacidadeMaxima(),
				quarto.getOcupacaoAtual(), quarto.getSituacao());
	}

}
