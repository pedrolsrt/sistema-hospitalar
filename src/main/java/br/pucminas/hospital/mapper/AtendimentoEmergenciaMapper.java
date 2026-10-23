package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.AtendimentoEmergenciaResponse;
import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.model.AtendimentoEmergencia;
import org.springframework.stereotype.Component;

@Component
public class AtendimentoEmergenciaMapper {

	public AtendimentoEmergenciaResponse paraResponse(AtendimentoEmergencia emergencia) {
		return new AtendimentoEmergenciaResponse(emergencia.getId(), Resumos.de(emergencia.getPaciente()),
				Resumos.de(emergencia.getProfissionalResponsavel()), CodigoDescricao.de(emergencia.getPrioridade()),
				emergencia.getDataHoraChegada(), CodigoDescricao.de(emergencia.getSituacao()),
				emergencia.getObservacoes());
	}

}
