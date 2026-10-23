package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.InternacaoResponse;
import br.pucminas.hospital.model.Internacao;
import org.springframework.stereotype.Component;

@Component
public class InternacaoMapper {

	public InternacaoResponse paraResponse(Internacao internacao) {
		return new InternacaoResponse(internacao.getId(), Resumos.de(internacao.getPaciente()),
				Resumos.de(internacao.getProfissionalResponsavel()), Resumos.de(internacao.getQuarto()),
				internacao.getDataEntrada(), internacao.getDataPrevistaAlta(), internacao.getDataEfetivaAlta(),
				internacao.isAtiva(), internacao.getObservacoes());
	}

}
