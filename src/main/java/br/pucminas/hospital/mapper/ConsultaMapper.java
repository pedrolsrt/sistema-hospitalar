package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.ConsultaResponse;
import br.pucminas.hospital.model.Consulta;
import org.springframework.stereotype.Component;

@Component
public class ConsultaMapper {

	public ConsultaResponse paraResponse(Consulta consulta) {
		return new ConsultaResponse(consulta.getId(), CodigoDescricao.de(consulta.getModalidade()),
				Resumos.de(consulta.getPaciente()), Resumos.de(consulta.getProfissionalResponsavel()), consulta.getData(),
				consulta.getHorario(), consulta.getMotivo(), CodigoDescricao.de(consulta.getAreaAtuacao()),
				CodigoDescricao.de(consulta.getSituacao()), consulta.getObservacoes());
	}

}
