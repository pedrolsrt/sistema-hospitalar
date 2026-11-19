package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.AtendimentoItemResponse;
import br.pucminas.hospital.dto.AtendimentoResponse;
import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.HistoricoMedicoResponse;
import br.pucminas.hospital.model.Atendimento;
import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.Internacao;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.model.VisitanteAtendimento;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Converte atendimentos de qualquer modalidade, para o histórico médico e para a busca de atendimentos.
 * Os dados próprios de cada modalidade vêm do mapper dela, escolhido pelo próprio atendimento via
 * {@link Atendimento#aceitar(VisitanteAtendimento)}, sem testes de tipo.
 */
@Component
public class AtendimentoMapper {

	private final VisitanteAtendimento<AtendimentoResponse> detalhes;

	public AtendimentoMapper(ConsultaMapper consultaMapper, InternacaoMapper internacaoMapper,
			AtendimentoEmergenciaMapper emergenciaMapper, ProcedimentoCirurgicoMapper procedimentoMapper) {
		this.detalhes = new VisitanteAtendimento<>() {

			@Override
			public AtendimentoResponse visitar(Consulta consulta) {
				return consultaMapper.paraResponse(consulta);
			}

			@Override
			public AtendimentoResponse visitar(Internacao internacao) {
				return internacaoMapper.paraResponse(internacao);
			}

			@Override
			public AtendimentoResponse visitar(AtendimentoEmergencia emergencia) {
				return emergenciaMapper.paraResponse(emergencia);
			}

			@Override
			public AtendimentoResponse visitar(ProcedimentoCirurgico procedimento) {
				return procedimentoMapper.paraResponse(procedimento);
			}

		};
	}

	public AtendimentoItemResponse paraItem(Atendimento atendimento) {
		return new AtendimentoItemResponse(atendimento.getDataHoraReferencia(),
				CodigoDescricao.de(atendimento.getModalidade()), CodigoDescricao.de(atendimento.getSituacao()),
				atendimento.aceitar(detalhes));
	}

	/**
	 * @param atendimentos já em ordem cronológica
	 */
	public HistoricoMedicoResponse paraHistorico(Paciente paciente, List<Atendimento> atendimentos) {
		return new HistoricoMedicoResponse(Resumos.de(paciente), atendimentos.stream().map(this::paraItem).toList());
	}

}
