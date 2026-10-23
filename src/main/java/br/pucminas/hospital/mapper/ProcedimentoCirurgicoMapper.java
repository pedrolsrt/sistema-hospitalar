package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoResponse;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import org.springframework.stereotype.Component;

@Component
public class ProcedimentoCirurgicoMapper {

	public ProcedimentoCirurgicoResponse paraResponse(ProcedimentoCirurgico procedimento) {
		return new ProcedimentoCirurgicoResponse(procedimento.getId(), Resumos.de(procedimento.getPaciente()),
				Resumos.de(procedimento.getProfissionalResponsavel()), procedimento.getDescricao(),
				CodigoDescricao.de(procedimento.getTipoCirurgia()), procedimento.getDataHoraPrevista(),
				procedimento.getDataHoraInicio(), procedimento.getDataHoraFim(),
				CodigoDescricao.de(procedimento.getSituacao()), procedimento.getObservacoesPreOperatorias(),
				procedimento.getObservacoesPosOperatorias(), procedimento.getObservacoes());
	}

}
