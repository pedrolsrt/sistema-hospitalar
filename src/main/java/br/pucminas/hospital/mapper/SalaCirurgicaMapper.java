package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.SalaCirurgicaRequest;
import br.pucminas.hospital.dto.SalaCirurgicaResponse;
import br.pucminas.hospital.model.SalaCirurgica;
import org.springframework.stereotype.Component;

@Component
public class SalaCirurgicaMapper {

	public SalaCirurgica paraEntidade(SalaCirurgicaRequest request) {
		return new SalaCirurgica(request.identificacao().strip());
	}

	public void atualizar(SalaCirurgica sala, SalaCirurgicaRequest request) {
		sala.setIdentificacao(request.identificacao().strip());
	}

	public SalaCirurgicaResponse paraResponse(SalaCirurgica sala) {
		return new SalaCirurgicaResponse(sala.getId(), sala.getIdentificacao(), sala.isAtiva());
	}

}
