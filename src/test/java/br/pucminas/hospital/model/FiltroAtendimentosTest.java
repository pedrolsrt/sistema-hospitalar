package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.DataInvalidaException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Um atendimento de cada modalidade, de dois pacientes e três profissionais, em datas diferentes.
 */
class FiltroAtendimentosTest {

	private static final LocalDate DIA = LocalDate.of(2030, 3, 10);

	private final Paciente maria = comId(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)), 1L);
	private final Paciente jose = comId(new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1)), 2L);
	private final ProfissionalSaude clinico = comId(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1"), 3L);
	private final Cirurgiao cirurgiao = comId(
			new Cirurgiao("Dra. Carla Mendes", "CRM-MG 2", AreaAtuacao.CIRURGIA_GERAL), 4L);

	private final Consulta consultaDaMaria = new ConsultaRotina(maria, clinico, DIA, LocalTime.of(9, 0), "Retorno");
	private final ProcedimentoCirurgico cirurgiaDaMaria = new ProcedimentoCirurgico(maria, cirurgiao, "Apendicectomia",
			TipoCirurgia.URGENCIA, new SalaCirurgica("Sala 1"), DIA.plusDays(2).atTime(8, 0), 60);
	private final Internacao internacaoDoJose = new Internacao(jose, clinico, new Quarto("101", 1, TipoQuarto.UTI, 1),
			DIA.minusDays(3), DIA.plusDays(1));
	private final AtendimentoEmergencia emergenciaDoJose = new AtendimentoEmergencia(jose, clinico,
			PrioridadeEmergencia.ALTA, DIA.plusDays(5).atTime(22, 0));

	private final List<Atendimento> todos = List.of(consultaDaMaria, cirurgiaDaMaria, internacaoDoJose,
			emergenciaDoJose);

	private static <T> T comId(T entidade, Long id) {
		ReflectionTestUtils.setField(entidade, "id", id);
		return entidade;
	}

	private List<Atendimento> filtrar(FiltroAtendimentos filtro) {
		return todos.stream().filter(filtro.comoPredicado()).toList();
	}

	// Cada predicado isolado ---------------------------------------------------------------------------

	@Test
	void doPaciente() {
		assertThat(todos.stream().filter(FiltroAtendimentos.doPaciente(1L)))
				.containsExactly(consultaDaMaria, cirurgiaDaMaria);
	}

	@Test
	void doProfissional() {
		assertThat(todos.stream().filter(FiltroAtendimentos.doProfissional(4L))).containsExactly(cirurgiaDaMaria);
	}

	@Test
	void naSituacaoComparaOCodigoDaSituacaoDeCadaModalidade() {
		assertThat(todos.stream().filter(FiltroAtendimentos.naSituacao("AGENDADA"))).containsExactly(consultaDaMaria);
		assertThat(todos.stream().filter(FiltroAtendimentos.naSituacao("AGENDADO"))).containsExactly(cirurgiaDaMaria);
		assertThat(todos.stream().filter(FiltroAtendimentos.naSituacao("ATIVA"))).containsExactly(internacaoDoJose);
		assertThat(todos.stream().filter(FiltroAtendimentos.naSituacao("AGUARDANDO_AVALIACAO")))
				.containsExactly(emergenciaDoJose);
		assertThat(todos.stream().filter(FiltroAtendimentos.naSituacao("INEXISTENTE"))).isEmpty();
	}

	@Test
	void daModalidade() {
		assertThat(todos.stream().filter(FiltroAtendimentos.daModalidade(ModalidadeAtendimento.INTERNACAO)))
				.containsExactly(internacaoDoJose);
	}

	@Test
	void periodoIncluiOsDoisLimites() {
		assertThat(todos.stream().filter(FiltroAtendimentos.aPartirDe(DIA))).containsExactly(consultaDaMaria,
				cirurgiaDaMaria, emergenciaDoJose);
		assertThat(todos.stream().filter(FiltroAtendimentos.ate(DIA))).containsExactly(consultaDaMaria,
				internacaoDoJose);
	}

	// Combinação --------------------------------------------------------------------------------------

	@Test
	void semCriteriosNaoFiltraNada() {
		assertThat(filtrar(new FiltroAtendimentos(null, null, null, null, null, null))).isEqualTo(todos);
	}

	@Test
	void combinaSoOsCriteriosInformados() {
		assertThat(filtrar(new FiltroAtendimentos(1L, null, null, null, DIA, DIA.plusDays(2))))
				.containsExactly(consultaDaMaria, cirurgiaDaMaria);
		assertThat(filtrar(new FiltroAtendimentos(2L, 3L, "ATIVA", ModalidadeAtendimento.INTERNACAO, null, null)))
				.containsExactly(internacaoDoJose);
		assertThat(filtrar(new FiltroAtendimentos(null, 3L, null, null, DIA, DIA.plusDays(1))))
				.containsExactly(consultaDaMaria);
	}

	@Test
	void todosOsCriteriosPrecisamSerAtendidos() {
		assertThat(filtrar(new FiltroAtendimentos(1L, null, null, ModalidadeAtendimento.EMERGENCIA, null, null)))
				.isEmpty();
	}

	@Test
	void situacoesReconhecidasReunemOsCodigosDeTodasAsModalidadesSemRepetir() {
		assertThat(FiltroAtendimentos.situacoesReconhecidas()).containsExactly("AGENDADA", "CONFIRMADA",
				"EM_ATENDIMENTO", "CONCLUIDA", "CANCELADA", "NAO_COMPARECEU", "AGUARDANDO_AVALIACAO", "CONCLUIDO",
				"AGENDADO", "EM_ANDAMENTO", "CANCELADO", "ATIVA", "ENCERRADA");
	}

	@Test
	void naoAceitaInicioDoPeriodoPosteriorAoFim() {
		assertThatThrownBy(() -> new FiltroAtendimentos(null, null, null, null, DIA, DIA.minusDays(1)))
				.isInstanceOf(DataInvalidaException.class)
				.hasMessage("O início do período (10/03/2030) não pode ser posterior ao fim (09/03/2030).");
	}

}
