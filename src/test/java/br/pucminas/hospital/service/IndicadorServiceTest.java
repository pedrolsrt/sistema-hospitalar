package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.AtendimentosPorModalidadeResponse;
import br.pucminas.hospital.dto.AtendimentosPorProfissionalResponse;
import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.IndicadoresResponse;
import br.pucminas.hospital.dto.QuartosPorSituacaoResponse;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.ConsultaEspecializada;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.Internacao;
import br.pucminas.hospital.model.MedicoEspecialista;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.PrioridadeEmergencia;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
import br.pucminas.hospital.repository.AtendimentoRepository;
import br.pucminas.hospital.repository.QuartoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IndicadorServiceTest {

	private static final LocalDate DIA = LocalDate.of(2030, 3, 10);

	@Mock
	private AtendimentoRepository atendimentoRepository;

	@Mock
	private QuartoRepository quartoRepository;

	private IndicadorService service;

	private ProfissionalSaude clinico;
	private ProfissionalSaude cardiologista;

	@BeforeEach
	void configurar() {
		service = new IndicadorService(atendimentoRepository, quartoRepository);
		clinico = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		cardiologista = new MedicoEspecialista("Dr. João Souza", "CRM-MG 2", AreaAtuacao.CARDIOLOGIA);
		ReflectionTestUtils.setField(clinico, "id", 1L);
		ReflectionTestUtils.setField(cardiologista, "id", 2L);
	}

	// Clínico: consulta concluída e consulta agendada no DIA, internação dois dias antes.
	// Cardiologista: consulta especializada concluída cinco dias depois e emergência no DIA.
	// Quartos: UTI ocupada pela internação, enfermaria e apartamento com vaga.
	private void comDadosDeExemplo() {
		Paciente maria = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
		Paciente jose = new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1));
		Quarto uti = new Quarto("201", 2, TipoQuarto.UTI, 1);
		Quarto enfermaria = new Quarto("101", 1, TipoQuarto.ENFERMARIA, 4);
		Quarto apartamento = new Quarto("102", 1, TipoQuarto.APARTAMENTO, 2);
		Consulta concluidaNoDia = concluida(new ConsultaRotina(maria, clinico, DIA, LocalTime.of(9, 0), "Check-up"));
		Consulta agendadaNoDia = new ConsultaRotina(jose, clinico, DIA, LocalTime.of(14, 0), "Retorno");
		Consulta concluidaDepois = concluida(new ConsultaEspecializada(jose, cardiologista, DIA.plusDays(5),
				LocalTime.of(10, 0), "Arritmia", AreaAtuacao.CARDIOLOGIA));
		Internacao internacao = new Internacao(jose, clinico, uti, DIA.minusDays(2), DIA.plusDays(2));
		AtendimentoEmergencia emergencia = new AtendimentoEmergencia(maria, cardiologista, PrioridadeEmergencia.ALTA,
				DIA.atTime(22, 0));
		when(atendimentoRepository.findAll())
				.thenReturn(List.of(concluidaNoDia, agendadaNoDia, concluidaDepois, internacao, emergencia));
		when(quartoRepository.findAll()).thenReturn(List.of(uti, enfermaria, apartamento));
	}

	private static Consulta concluida(Consulta consulta) {
		consulta.confirmar();
		consulta.iniciarAtendimento();
		consulta.concluir();
		return consulta;
	}

	private static List<Long> quantidadesPorModalidade(IndicadoresResponse indicadores) {
		return indicadores.atendimentosPorModalidade().stream().map(AtendimentosPorModalidadeResponse::quantidade)
				.toList();
	}

	@Test
	void semPeriodoContaTodosOsAtendimentos() {
		comDadosDeExemplo();

		IndicadoresResponse indicadores = service.calcular(null, null);

		assertThat(indicadores.inicio()).isNull();
		assertThat(indicadores.fim()).isNull();
		assertThat(indicadores.consultasRealizadas()).isEqualTo(2);
		assertThat(indicadores.atendimentosPorModalidade()).containsExactly(
				new AtendimentosPorModalidadeResponse(new CodigoDescricao("CONSULTA_ROTINA", "Consulta de rotina"), 2),
				new AtendimentosPorModalidadeResponse(
						new CodigoDescricao("CONSULTA_ESPECIALIZADA", "Consulta especializada"), 1),
				new AtendimentosPorModalidadeResponse(new CodigoDescricao("EMERGENCIA", "Atendimento de emergência"), 1),
				new AtendimentosPorModalidadeResponse(
						new CodigoDescricao("PROCEDIMENTO_CIRURGICO", "Procedimento cirúrgico"), 0),
				new AtendimentosPorModalidadeResponse(new CodigoDescricao("INTERNACAO", "Internação"), 1));
		assertThat(indicadores.quartos()).isEqualTo(new QuartosPorSituacaoResponse(2, 1));
		assertThat(indicadores.atendimentosPorProfissional()).containsExactly(
				new AtendimentosPorProfissionalResponse(1L, "Dra. Ana Lima",
						new CodigoDescricao("CLINICO_GERAL", "Clínico geral"), 3),
				new AtendimentosPorProfissionalResponse(2L, "Dr. João Souza",
						new CodigoDescricao("MEDICO_ESPECIALISTA", "Médico especialista"), 2));
	}

	@Test
	void consultaRealizadaESoAConcluida() {
		comDadosDeExemplo();

		IndicadoresResponse indicadores = service.calcular(DIA, DIA);

		// No DIA há duas consultas de rotina, mas só uma foi concluída.
		assertThat(indicadores.consultasRealizadas()).isEqualTo(1);
		assertThat(quantidadesPorModalidade(indicadores)).containsExactly(2L, 0L, 1L, 0L, 0L);
	}

	@Test
	void periodoIncluiAsDuasDatas() {
		comDadosDeExemplo();

		IndicadoresResponse indicadores = service.calcular(DIA.minusDays(2), DIA.plusDays(5));

		// A internação começa no primeiro dia e a consulta especializada é no último.
		assertThat(indicadores.consultasRealizadas()).isEqualTo(2);
		assertThat(quantidadesPorModalidade(indicadores)).containsExactly(2L, 1L, 1L, 0L, 1L);
	}

	@Test
	void periodoSemAtendimentosZeraOsIndicadoresDeAtendimento() {
		comDadosDeExemplo();

		IndicadoresResponse indicadores = service.calcular(DIA.plusDays(1), DIA.plusDays(4));

		assertThat(indicadores.consultasRealizadas()).isZero();
		assertThat(quantidadesPorModalidade(indicadores)).containsOnly(0L);
		assertThat(indicadores.atendimentosPorProfissional()).isEmpty();
	}

	@Test
	void periodoComSoInicioOuSoFim() {
		comDadosDeExemplo();

		IndicadoresResponse aPartirDoDiaSeguinte = service.calcular(DIA.plusDays(1), null);
		IndicadoresResponse ateAVespera = service.calcular(null, DIA.minusDays(1));

		assertThat(aPartirDoDiaSeguinte.consultasRealizadas()).isEqualTo(1);
		assertThat(quantidadesPorModalidade(aPartirDoDiaSeguinte)).containsExactly(0L, 1L, 0L, 0L, 0L);
		assertThat(ateAVespera.consultasRealizadas()).isZero();
		assertThat(quantidadesPorModalidade(ateAVespera)).containsExactly(0L, 0L, 0L, 0L, 1L);
	}

	@Test
	void quartosMostramASituacaoAtualMesmoComPeriodo() {
		comDadosDeExemplo();

		assertThat(service.calcular(DIA.plusDays(1), DIA.plusDays(4)).quartos())
				.isEqualTo(new QuartosPorSituacaoResponse(2, 1));
	}

	@Test
	void profissionaisDoPeriodoEmOrdemDeAtendimentosDepoisDeNome() {
		comDadosDeExemplo();

		// No DIA: clínico com duas consultas, cardiologista com a emergência.
		assertThat(service.calcular(DIA, DIA).atendimentosPorProfissional())
				.extracting(AtendimentosPorProfissionalResponse::nome, AtendimentosPorProfissionalResponse::quantidade)
				.containsExactly(tuple("Dra. Ana Lima", 2L), tuple("Dr. João Souza", 1L));
		// Do dia seguinte em diante, só o cardiologista; empatados, a ordem seria pelo nome.
		assertThat(service.calcular(DIA.plusDays(1), null).atendimentosPorProfissional())
				.extracting(AtendimentosPorProfissionalResponse::nome).containsExactly("Dr. João Souza");
	}

	@Test
	void semDadosTudoZeradoComTodasAsModalidades() {
		when(atendimentoRepository.findAll()).thenReturn(List.of());
		when(quartoRepository.findAll()).thenReturn(List.of());

		IndicadoresResponse indicadores = service.calcular(null, null);

		assertThat(indicadores.consultasRealizadas()).isZero();
		assertThat(indicadores.atendimentosPorModalidade()).hasSize(5);
		assertThat(quantidadesPorModalidade(indicadores)).containsOnly(0L);
		assertThat(indicadores.quartos()).isEqualTo(new QuartosPorSituacaoResponse(0, 0));
		assertThat(indicadores.atendimentosPorProfissional()).isEmpty();
	}

	@Test
	void recusaInicioPosteriorAoFimSemConsultarOBanco() {
		assertThatThrownBy(() -> service.calcular(DIA.plusDays(1), DIA))
				.isInstanceOf(DataInvalidaException.class)
				.hasMessage("O início do período (11/03/2030) não pode ser posterior ao fim (10/03/2030).");
		verifyNoInteractions(atendimentoRepository, quartoRepository);
	}

	@Test
	void contarAplicaOFiltroEAgrupaPeloCriterio() {
		assertThat(IndicadorService.contar(List.of("a", "bb", "cc", "ddd"), texto -> !texto.equals("ddd"),
				String::length))
				.containsOnly(entry(1, 1L), entry(2, 2L));
	}

}
