package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.DominiosResponse;
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
import org.junit.jupiter.api.Test;

class DominioServiceTest {

	private final DominiosResponse dominios = new DominioService().listar();

	private static List<String> codigos(List<CodigoDescricao> valores) {
		return valores.stream().map(CodigoDescricao::codigo).toList();
	}

	private static List<String> nomes(Descritivel[] constantes) {
		return Stream.of(constantes).map(Descritivel::name).toList();
	}

	@Test
	void cadaListaTrazTodosOsValoresDoEnumNaOrdemDeclarada() {
		assertThat(codigos(dominios.situacoesConsulta())).isEqualTo(nomes(SituacaoConsulta.values()));
		assertThat(codigos(dominios.situacoesEmergencia())).isEqualTo(nomes(SituacaoEmergencia.values()));
		assertThat(codigos(dominios.situacoesProcedimento())).isEqualTo(nomes(SituacaoProcedimento.values()));
		assertThat(codigos(dominios.situacoesInternacao())).isEqualTo(nomes(SituacaoInternacao.values()));
		assertThat(codigos(dominios.modalidades())).isEqualTo(nomes(ModalidadeAtendimento.values()));
		assertThat(codigos(dominios.categoriasProfissional())).isEqualTo(nomes(CategoriaProfissional.values()));
		assertThat(codigos(dominios.areasAtuacao())).isEqualTo(nomes(AreaAtuacao.values()));
		assertThat(codigos(dominios.tiposQuarto())).isEqualTo(nomes(TipoQuarto.values()));
		assertThat(codigos(dominios.prioridadesEmergencia())).isEqualTo(nomes(PrioridadeEmergencia.values()));
		assertThat(codigos(dominios.tiposCirurgia())).isEqualTo(nomes(TipoCirurgia.values()));
		assertThat(codigos(dominios.ordenacoes())).isEqualTo(nomes(OrdenacaoAtendimentos.values()));
	}

	@Test
	void cadaValorTrazADescricaoLegivel() {
		assertThat(dominios.situacoesConsulta()).contains(new CodigoDescricao("NAO_COMPARECEU", "Não compareceu"));
		assertThat(dominios.situacoesInternacao()).containsExactly(new CodigoDescricao("ATIVA", "Ativa"),
				new CodigoDescricao("ENCERRADA", "Encerrada"));
		assertThat(dominios.areasAtuacao()).contains(new CodigoDescricao("CIRURGIA_GERAL", "Cirurgia geral"),
				new CodigoDescricao("CIRURGIA_CARDIOVASCULAR", "Cirurgia cardiovascular"));
		assertThat(dominios.prioridadesEmergencia()).containsExactly(new CodigoDescricao("BAIXA", "Baixa"),
				new CodigoDescricao("MODERADA", "Moderada"), new CodigoDescricao("ALTA", "Alta"),
				new CodigoDescricao("CRITICA", "Crítica"));
	}

	@Test
	void nenhumaAreaDeAtuacaoTemMaiusculaNoMeioDaDescricao() {
		assertThat(dominios.areasAtuacao())
				.extracting(CodigoDescricao::descricao)
				.allSatisfy(descricao -> assertThat(descricao.substring(1)).isEqualTo(descricao.substring(1).toLowerCase()));
	}

}
