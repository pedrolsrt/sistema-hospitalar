package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.AtendimentoItemResponse;
import br.pucminas.hospital.mapper.AtendimentoEmergenciaMapper;
import br.pucminas.hospital.mapper.AtendimentoMapper;
import br.pucminas.hospital.mapper.ConsultaMapper;
import br.pucminas.hospital.mapper.InternacaoMapper;
import br.pucminas.hospital.mapper.ProcedimentoCirurgicoMapper;
import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.FiltroAtendimentos;
import br.pucminas.hospital.model.Internacao;
import br.pucminas.hospital.model.ModalidadeAtendimento;
import br.pucminas.hospital.model.OrdenacaoAtendimentos;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.PrioridadeEmergencia;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
import br.pucminas.hospital.repository.AtendimentoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

	private static final LocalDate DIA = LocalDate.of(2030, 3, 10);

	@Mock
	private AtendimentoRepository repository;

	private AtendimentoService service;

	private ConsultaRotina consulta;
	private Internacao internacao;
	private AtendimentoEmergencia emergencia;

	@BeforeEach
	void configurar() {
		service = new AtendimentoService(repository, new AtendimentoMapper(new ConsultaMapper(), new InternacaoMapper(),
				new AtendimentoEmergenciaMapper(), new ProcedimentoCirurgicoMapper()));
		Paciente maria = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
		Paciente jose = new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1));
		ProfissionalSaude clinico = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		consulta = new ConsultaRotina(maria, clinico, DIA, LocalTime.of(9, 0), "Retorno");
		internacao = new Internacao(jose, clinico, new Quarto("101", 1, TipoQuarto.UTI, 1), DIA.minusDays(2),
				DIA.plusDays(2));
		emergencia = new AtendimentoEmergencia(maria, clinico, PrioridadeEmergencia.ALTA, DIA.atTime(22, 0));
		when(repository.findAll()).thenReturn(List.of(emergencia, consulta, internacao));
	}

	@Test
	void semFiltroDevolveTodasAsModalidadesNaOrdemPedida() {
		FiltroAtendimentos semFiltro = new FiltroAtendimentos(null, null, null, null, null, null);

		assertThat(service.listar(semFiltro, OrdenacaoAtendimentos.DATA))
				.extracting(item -> item.modalidade().codigo())
				.containsExactly("INTERNACAO", "CONSULTA_ROTINA", "EMERGENCIA");
		assertThat(service.listar(semFiltro, OrdenacaoAtendimentos.PACIENTE))
				.extracting(item -> item.detalhes().paciente().nome())
				.containsExactly("José Lima", "Maria Silva", "Maria Silva");
		assertThat(service.listar(semFiltro, OrdenacaoAtendimentos.PRIORIDADE))
				.extracting(item -> item.modalidade().codigo())
				.containsExactly("EMERGENCIA", "INTERNACAO", "CONSULTA_ROTINA");
	}

	@Test
	void aplicaOFiltroEDevolveASituacaoDeCadaAtendimento() {
		List<AtendimentoItemResponse> internacoesAtivas = service.listar(
				new FiltroAtendimentos(null, null, "ATIVA", ModalidadeAtendimento.INTERNACAO, null, null),
				OrdenacaoAtendimentos.DATA);

		assertThat(internacoesAtivas).hasSize(1);
		assertThat(internacoesAtivas.get(0).situacao().descricao()).isEqualTo("Ativa");
		assertThat(internacoesAtivas.get(0).dataHora()).isEqualTo(DIA.minusDays(2).atStartOfDay());
	}

}
