package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.QuartoRequest;
import br.pucminas.hospital.dto.QuartoResponse;
import br.pucminas.hospital.model.Quarto;
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
		return new QuartoResponse(quarto.getId(), quarto.getNumero(), quarto.getAndar(),
				CodigoDescricao.de(quarto.getTipo()), quarto.getCapacidadeMaxima(),
				quarto.getOcupacaoAtual(), quarto.getSituacao());
	}

}
