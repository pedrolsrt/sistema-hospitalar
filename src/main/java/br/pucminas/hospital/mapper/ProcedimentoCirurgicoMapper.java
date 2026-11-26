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
				CodigoDescricao.de(procedimento.getTipoCirurgia()), Resumos.de(procedimento.getSalaCirurgica()),
				procedimento.getDataHoraPrevista(), procedimento.getDuracaoPrevistaMinutos(),
				procedimento.getPeriodoPrevisto().fim(), procedimento.getDataHoraInicio(),
				procedimento.getDataHoraFim(), CodigoDescricao.de(procedimento.getSituacao()),
				procedimento.getObservacoesPreOperatorias(), procedimento.getObservacoesPosOperatorias(),
				procedimento.getObservacoes(),
				procedimento.getSituacao().getProximasPossiveis().stream().map(CodigoDescricao::de).toList(),
				procedimento.getSituacao().permiteObservacoesPreOperatorias(),
				procedimento.getSituacao().permiteObservacoesPosOperatorias());
	}

}
