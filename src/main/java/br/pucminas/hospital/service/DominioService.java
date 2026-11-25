package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.CategoriaProfissionalResponse;
import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.DominiosResponse;
import br.pucminas.hospital.dto.TipoQuartoResponse;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.CategoriaProfissional;
import br.pucminas.hospital.model.Descritivel;
import br.pucminas.hospital.model.ModalidadeAtendimento;
import br.pucminas.hospital.model.OrdenacaoAtendimentos;
import br.pucminas.hospital.model.PrioridadeEmergencia;
import br.pucminas.hospital.model.SituacaoConsulta;
import br.pucminas.hospital.model.SituacaoEmergencia;
import br.pucminas.hospital.model.SituacaoInternacao;
import br.pucminas.hospital.model.SituacaoProcedimento;
import br.pucminas.hospital.model.TipoCirurgia;
import br.pucminas.hospital.model.TipoQuarto;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Valores dos enums que as telas usam, no formato {@link CodigoDescricao}. Os valores vêm dos próprios enums,
 * então uma constante nova aparece aqui sem outra alteração.
 */
@Service
public class DominioService {

	public DominiosResponse listar() {
		return new DominiosResponse(
				valores(SituacaoConsulta.values()),
				valores(SituacaoEmergencia.values()),
				valores(SituacaoProcedimento.values()),
				valores(SituacaoInternacao.values()),
				valores(ModalidadeAtendimento.values()),
				Stream.of(CategoriaProfissional.values())
						.map(categoria -> new CategoriaProfissionalResponse(categoria.name(), categoria.getDescricao(),
								categoria.exigeArea()))
						.toList(),
				valores(AreaAtuacao.values()),
				Stream.of(TipoQuarto.values())
						.map(tipo -> new TipoQuartoResponse(tipo.name(), tipo.getDescricao(),
								tipo.getCapacidadeMaximaPermitida()))
						.toList(),
				valores(PrioridadeEmergencia.values()),
				valores(TipoCirurgia.values()),
				valores(OrdenacaoAtendimentos.values()));
	}

	private static List<CodigoDescricao> valores(Descritivel[] constantes) {
		return Stream.of(constantes).map(CodigoDescricao::de).toList();
	}

}
