package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.exception.AtendimentoIndisponivelException;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.Cirurgiao;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.Periodo;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.SalaCirurgica;
import br.pucminas.hospital.model.TipoCirurgia;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Conflitos entre consultas e procedimentos para o profissional, o paciente e a sala. As consultas duram
 * 30 minutos; os procedimentos, a duração prevista de cada um.
 */
@ExtendWith(MockitoExtension.class)
class VerificadorDeDisponibilidadeTest {

	private static final LocalDate DIA = LocalDate.of(2030, 3, 10);

	@Mock
	private ConsultaRepository consultaRepository;

	@Mock
	private ProcedimentoCirurgicoRepository procedimentoRepository;

	private VerificadorDeDisponibilidade verificador;

	private Paciente maria;
	private Paciente jose;
	private ProfissionalSaude clinico;
	private Cirurgiao draCarla;
	private Cirurgiao drPaulo;
	private SalaCirurgica sala1;
	private SalaCirurgica sala2;

	@BeforeEach
	void configurar() {
		verificador = new VerificadorDeDisponibilidade(consultaRepository, procedimentoRepository, 30);
		maria = comId(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)), 1L);
		jose = comId(new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1)), 2L);
		clinico = comId(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1"), 3L);
		draCarla = comId(new Cirurgiao("Dra. Carla Mendes", "CRM-MG 2", AreaAtuacao.CIRURGIA_GERAL), 4L);
		drPaulo = comId(new Cirurgiao("Dr. Paulo Reis", "CRM-MG 3", AreaAtuacao.ORTOPEDIA), 5L);
		sala1 = comId(new SalaCirurgica("Sala 1"), 6L);
		sala2 = comId(new SalaCirurgica("Sala 2"), 7L);
	}

	private static <T> T comId(T entidade, Long id) {
		ReflectionTestUtils.setField(entidade, "id", id);
		return entidade;
	}

	private static Consulta consulta(ProfissionalSaude profissional, Paciente paciente, String horario) {
		return new ConsultaRotina(paciente, profissional, DIA, LocalTime.parse(horario), "Retorno");
	}

	private static ProcedimentoCirurgico procedimento(Cirurgiao cirurgiao, Paciente paciente, SalaCirurgica sala,
			String horario, int minutos) {
		return new ProcedimentoCirurgico(paciente, cirurgiao, "Apendicectomia", TipoCirurgia.ELETIVA, sala,
				DIA.atTime(LocalTime.parse(horario)), minutos);
	}

	// Consulta pedida -----------------------------------------------------------------------------------

	@Test
	void consultaLivreQuandoNaoHaOcupacoes() {
		assertThatCode(() -> verificador.exigirHorarioLivre(consulta(draCarla, maria, "10:00")))
				.doesNotThrowAnyException();
	}

	@Test
	void consultaDoCirurgiaoNaoPodeCoincidirComProcedimentoDele() {
		when(procedimentoRepository.findAtivosDoProfissionalProximosDe(eq(4L), eq(periodo("10:00", "10:30"))))
				.thenReturn(List.of(procedimento(draCarla, jose, sala1, "09:00", 90)));

		assertThatThrownBy(() -> verificador.exigirHorarioLivre(consulta(draCarla, maria, "10:00")))
				.isInstanceOf(AtendimentoIndisponivelException.class)
				.hasMessage("O profissional Dra. Carla Mendes já tem procedimento cirúrgico em 10/03/2030, "
						+ "das 09:00 às 10:30, que se sobrepõe ao período pedido (10/03/2030, das 10:00 às 10:30).");
	}

	@Test
	void consultaDoPacienteNaoPodeCoincidirComProcedimentoDele() {
		when(procedimentoRepository.findAtivosDoPacienteProximosDe(eq(1L), eq(periodo("10:00", "10:30"))))
				.thenReturn(List.of(procedimento(draCarla, maria, sala1, "10:15", 60)));

		assertThatThrownBy(() -> verificador.exigirHorarioLivre(consulta(clinico, maria, "10:00")))
				.isInstanceOf(AtendimentoIndisponivelException.class)
				.hasMessageStartingWith(
						"O paciente Maria Silva já tem procedimento cirúrgico em 10/03/2030, das 10:15");
	}

	@Test
	void consultaPodeComecarQuandoOProcedimentoTermina() {
		when(procedimentoRepository.findAtivosDoProfissionalProximosDe(eq(4L), eq(periodo("10:30", "11:00"))))
				.thenReturn(List.of(procedimento(draCarla, jose, sala1, "09:00", 90)));

		assertThatCode(() -> verificador.exigirHorarioLivre(consulta(draCarla, maria, "10:30")))
				.doesNotThrowAnyException();
	}

	// Procedimento pedido -------------------------------------------------------------------------------

	@Test
	void procedimentoLivreQuandoNaoHaOcupacoes() {
		assertThatCode(() -> verificador.exigirHorarioLivre(procedimento(draCarla, maria, sala1, "08:00", 120)))
				.doesNotThrowAnyException();
	}

	@Test
	void procedimentoNaoPodeCoincidirComConsultaDoCirurgiao() {
		when(consultaRepository.findAtivasDoProfissionalProximasDe(eq(4L), eq(periodo("08:00", "10:00"))))
				.thenReturn(List.of(consulta(draCarla, jose, "09:45")));

		assertThatThrownBy(() -> verificador.exigirHorarioLivre(procedimento(draCarla, maria, sala1, "08:00", 120)))
				.isInstanceOf(AtendimentoIndisponivelException.class)
				.hasMessage("O profissional Dra. Carla Mendes já tem consulta em 10/03/2030, das 09:45 às 10:15, "
						+ "que se sobrepõe ao período pedido (10/03/2030, das 08:00 às 10:00).");
	}

	@Test
	void procedimentoNaoPodeCoincidirComConsultaDoPaciente() {
		when(consultaRepository.findAtivasDoPacienteProximasDe(eq(1L), eq(periodo("08:00", "10:00"))))
				.thenReturn(List.of(consulta(clinico, maria, "07:45")));

		assertThatThrownBy(() -> verificador.exigirHorarioLivre(procedimento(draCarla, maria, sala1, "08:00", 120)))
				.isInstanceOf(AtendimentoIndisponivelException.class)
				.hasMessageStartingWith("O paciente Maria Silva já tem consulta em 10/03/2030, das 07:45 às 08:15");
	}

	@Test
	void procedimentoNaoPodeCoincidirComOutroProcedimentoDoCirurgiao() {
		when(procedimentoRepository.findAtivosDoProfissionalProximosDe(eq(4L), eq(periodo("08:00", "10:00"))))
				.thenReturn(List.of(procedimento(draCarla, jose, sala2, "09:30", 60)));

		assertThatThrownBy(() -> verificador.exigirHorarioLivre(procedimento(draCarla, maria, sala1, "08:00", 120)))
				.isInstanceOf(AtendimentoIndisponivelException.class)
				.hasMessageStartingWith("O profissional Dra. Carla Mendes já tem procedimento cirúrgico");
	}

	@Test
	void procedimentoNaoPodeCoincidirComOutroProcedimentoDoPaciente() {
		when(procedimentoRepository.findAtivosDoPacienteProximosDe(eq(1L), eq(periodo("08:00", "10:00"))))
				.thenReturn(List.of(procedimento(drPaulo, maria, sala2, "07:00", 90)));

		assertThatThrownBy(() -> verificador.exigirHorarioLivre(procedimento(draCarla, maria, sala1, "08:00", 120)))
				.isInstanceOf(AtendimentoIndisponivelException.class)
				.hasMessageStartingWith("O paciente Maria Silva já tem procedimento cirúrgico");
	}

	@Test
	void salaNaoPodeTerDoisProcedimentosNoMesmoPeriodo() {
		when(procedimentoRepository.findAtivosDaSalaProximosDe(eq(6L), eq(periodo("08:00", "10:00"))))
				.thenReturn(List.of(procedimento(drPaulo, jose, sala1, "09:00", 60)));

		assertThatThrownBy(() -> verificador.exigirHorarioLivre(procedimento(draCarla, maria, sala1, "08:00", 120)))
				.isInstanceOf(AtendimentoIndisponivelException.class)
				.hasMessage("A sala cirúrgica Sala 1 já tem procedimento cirúrgico em 10/03/2030, das 09:00 às 10:00, "
						+ "que se sobrepõe ao período pedido (10/03/2030, das 08:00 às 10:00).");
	}

	// Procedimento de outra equipe na sala das 10:00 às 11:00; o pedido dura 2 horas.
	@ParameterizedTest(name = "início às {0}: livre = {1}")
	@CsvSource({ "08:00, true", "08:01, false", "09:00, false", "10:59, false", "11:00, true" })
	void limitesDaSobreposicaoNaSala(String inicio, boolean livre) {
		ProcedimentoCirurgico pedido = procedimento(draCarla, maria, sala1, inicio, 120);
		when(procedimentoRepository.findAtivosDaSalaProximosDe(6L, pedido.getPeriodoPrevisto()))
				.thenReturn(List.of(procedimento(drPaulo, jose, sala1, "10:00", 60)));

		if (livre) {
			assertThatCode(() -> verificador.exigirHorarioLivre(pedido)).doesNotThrowAnyException();
		} else {
			assertThatThrownBy(() -> verificador.exigirHorarioLivre(pedido))
					.isInstanceOf(AtendimentoIndisponivelException.class);
		}
	}

	@Test
	void buscaAsOcupacoesDaSalaDoProcedimentoPedido() {
		verificador.exigirHorarioLivre(procedimento(draCarla, maria, sala2, "08:00", 60));

		verify(procedimentoRepository).findAtivosDaSalaProximosDe(7L, periodo("08:00", "09:00"));
	}

	private static Periodo periodo(String inicio, String fim) {
		return new Periodo(LocalDateTime.of(DIA, LocalTime.parse(inicio)), LocalDateTime.of(DIA, LocalTime.parse(fim)));
	}

}
