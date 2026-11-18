package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoResponse;
import br.pucminas.hospital.model.Periodo;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import org.springframework.stereotype.Component;

@Component
public class ProcedimentoCirurgicoMapper {

	public ProcedimentoCirurgicoResponse paraResponse(ProcedimentoCirurgico procedimento) {
		Periodo previsto = procedimento.getPeriodoPrevisto();
		return new ProcedimentoCirurgicoResponse(procedimento.getId(), Resumos.de(procedimento.getPaciente()),
				Resumos.de(procedimento.getProfissionalResponsavel()), procedimento.getDescricao(),
				CodigoDescricao.de(procedimento.getTipoCirurgia()), Resumos.de(procedimento.getSalaCirurgica()),
				previsto.inicio(), procedimento.getDuracaoPrevistaMinutos(), previsto.fim(),
				procedimento.getDataHoraInicio(), procedimento.getDataHoraFim(),
				CodigoDescricao.de(procedimento.getSituacao()), procedimento.getObservacoesPreOperatorias(),
				procedimento.getObservacoesPosOperatorias(), procedimento.getObservacoes());
	}

}
