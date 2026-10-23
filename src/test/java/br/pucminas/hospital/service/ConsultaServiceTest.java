package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.ConsultaEspecializadaRequest;
import br.pucminas.hospital.dto.ConsultaResponse;
import br.pucminas.hospital.dto.ConsultaRotinaRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.mapper.ConsultaMapper;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.MedicoEspecialista;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.SituacaoConsulta;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

	private static final LocalDateTime AGORA = LocalDateTime.of(2030, 1, 10, 10, 0);
	private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

	@Mock
	private ConsultaRepository repository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private ProfissionalSaudeRepository profissionalRepository;

	private ConsultaService service;

	private Paciente maria;
	private ProfissionalSaude drJoao;
	private MedicoEspecialista cardiologista;

	@BeforeEach
	void configurar() {
		service = criarService(30);
		maria = comId(new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10)), 1L);
		drJoao = comId(new ClinicoGeral("Dr. João Souza", "CRM-MG 1"), 2L);
		cardiologista = comId(new MedicoEspecialista("Dra. Ana Lima", "CRM-MG 2", AreaAtuacao.CARDIOLOGIA), 3L);
		lenient().when(pacienteRepository.findById(1L)).thenReturn(Optional.of(maria));
		lenient().when(profissionalRepository.findById(2L)).thenReturn(Optional.of(drJoao));
		lenient().when(profissionalRepository.findById(3L)).thenReturn(Optional.of(cardiologista));
		lenient().when(repository.save(any(Consulta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
	}

	private ConsultaService criarService(long duracaoMinutos) {
		return new ConsultaService(repository, pacienteRepository, profissionalRepository, new ConsultaMapper(),
				Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC), duracaoMinutos);
	}

	private static <T> T comId(T entidade, Long id) {
		ReflectionTestUtils.setField(entidade, "id", id);
		return entidade;
	}

	private static ConsultaRotinaRequest rotina(LocalDate data, String horario) {
		return new ConsultaRotinaRequest(1L, 2L, data, LocalTime.parse(horario), "Check-up");
	}

	// Consulta já agendada, levada até a situação pedida.
	private static Consulta existente(ProfissionalSaude profissional, Paciente paciente, LocalDate data, String horario,
			SituacaoConsulta situacao) {
		Consulta consulta = new ConsultaRotina(paciente, profissional, data, LocalTime.parse(horario), "Retorno");
		if (situacao != SituacaoConsulta.AGENDADA) {
			consulta.confirmar();
		}
		if (situacao == SituacaoConsulta.EM_ATENDIMENTO) {
			consulta.iniciarAtendimento();
		}
		return consulta;
	}

	// Agendamento -------------------------------------------------------------------------------------

	@Test
	void agendaConsultaDeRotina() {
		ConsultaResponse resposta = service.agendarRotina(rotina(DIA, "14:00"));

		verify(repository).save(any(ConsultaRotina.class));
		assertThat(resposta.modalidade().codigo()).isEqualTo("CONSULTA_ROTINA");
		assertThat(resposta.situacao().codigo()).isEqualTo("AGENDADA");
		assertThat(resposta.profissional().nome()).isEqualTo("Dr. João Souza");
		assertThat(resposta.areaAtuacao()).isNull();
	}

	@Test
	void agendaConsultaEspecializadaComEspecialistaDaArea() {
		ConsultaResponse resposta = service.agendarEspecializada(new ConsultaEspecializadaRequest(1L, 3L, DIA,
				LocalTime.of(9, 0), "Dor no peito", AreaAtuacao.CARDIOLOGIA));

		assertThat(resposta.modalidade().codigo()).isEqualTo("CONSULTA_ESPECIALIZADA");
		assertThat(resposta.areaAtuacao().descricao()).isEqualTo("Cardiologia");
	}

	@Test
	void naoAgendaConsultaEspecializadaComProfissionalQueNaoEEspecialistaDaArea() {
		assertThatThrownBy(() -> service.agendarEspecializada(new ConsultaEspecializadaRequest(1L, 2L, DIA,
				LocalTime.of(9, 0), "Dor no peito", AreaAtuacao.CARDIOLOGIA)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("não pode realizar consulta especializada em Cardiologia");
		verify(repository, never()).save(any());
	}

	@Test
	void naoAgendaParaPacienteInativo() {
		maria.inativar();

		assertThatThrownBy(() -> service.agendarRotina(rotina(DIA, "14:00")))
				.hasMessage("O paciente Maria Silva está inativo e não pode ser atendido.");
		verify(repository, never()).save(any());
	}

	@Test
	void naoAgendaComProfissionalInativo() {
		drJoao.inativar();

		assertThatThrownBy(() -> service.agendarRotina(rotina(DIA, "14:00")))
				.hasMessage("O profissional Dr. João Souza está inativo e não pode realizar atendimentos.");
		verify(repository, never()).save(any());
	}

	@Test
	void naoAgendaComPacienteOuProfissionalInexistente() {
		assertThatThrownBy(() -> service.agendarRotina(new ConsultaRotinaRequest(9L, 2L, DIA, LocalTime.NOON, "X")))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Paciente não encontrado (id 9).");
		assertThatThrownBy(() -> service.agendarRotina(new ConsultaRotinaRequest(1L, 9L, DIA, LocalTime.NOON, "X")))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Profissional de saúde não encontrado (id 9).");
	}

	@ParameterizedTest
	@CsvSource({ "2030-01-09, 15:00", "2030-01-10, 09:59", "2030-01-10, 10:00" })
	void naoAgendaNoPassadoNemNoMomentoAtual(LocalDate data, String horario) {
		assertThatThrownBy(() -> service.agendarRotina(rotina(data, horario)))
				.isInstanceOf(DataInvalidaException.class)
				.hasMessageStartingWith("A consulta deve ser agendada para uma data e horário futuros");
		verify(repository, never()).save(any());
	}

	@Test
	void agendaParaLogoDepoisDoMomentoAtual() {
		assertThat(service.agendarRotina(rotina(AGORA.toLocalDate(), "10:01")).situacao().codigo())
				.isEqualTo("AGENDADA");
	}

	// Sobreposição de horários -------------------------------------------------------------------------

	@ParameterizedTest
	@EnumSource(value = SituacaoConsulta.class, names = { "AGENDADA", "CONFIRMADA", "EM_ATENDIMENTO" })
	void naoAgendaSobreConsultaAtivaDoProfissional(SituacaoConsulta situacao) {
		Paciente outro = comId(new Paciente("José Lima", "11144477735", LocalDate.of(1985, 2, 1)), 4L);
		when(repository.findAtivasDoProfissionalNaData(2L, DIA))
				.thenReturn(List.of(existente(drJoao, outro, DIA, "14:00", situacao)));

		assertThatThrownBy(() -> service.agendarRotina(rotina(DIA, "14:15")))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O profissional Dr. João Souza já tem consulta em 15/01/2030 às 14:00, que se sobrepõe ao "
						+ "horário pedido (cada consulta dura 30 minutos).");
		verify(repository, never()).save(any());
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoConsulta.class, names = { "AGENDADA", "CONFIRMADA", "EM_ATENDIMENTO" })
	void naoAgendaSobreConsultaAtivaDoPaciente(SituacaoConsulta situacao) {
		when(repository.findAtivasDoPacienteNaData(1L, DIA))
				.thenReturn(List.of(existente(cardiologista, maria, DIA, "14:00", situacao)));

		assertThatThrownBy(() -> service.agendarRotina(rotina(DIA, "13:45")))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageStartingWith("O paciente Maria Silva já tem consulta em 15/01/2030 às 14:00");
		verify(repository, never()).save(any());
	}

	// Consulta existente das 14:00 às 14:30: encostar no início ou no fim não é sobreposição.
	@ParameterizedTest(name = "{0}: livre = {1}")
	@CsvSource({ "13:30, true", "13:31, false", "14:00, false", "14:29, false", "14:30, true" })
	void limitesDaSobreposicao(String horario, boolean livre) {
		lenient().when(repository.findAtivasDoProfissionalNaData(2L, DIA))
				.thenReturn(List.of(existente(drJoao, maria, DIA, "14:00", SituacaoConsulta.AGENDADA)));

		if (livre) {
			assertThat(service.agendarRotina(rotina(DIA, horario)).horario()).isEqualTo(LocalTime.parse(horario));
		} else {
			assertThatThrownBy(() -> service.agendarRotina(rotina(DIA, horario)))
					.isInstanceOf(RegraNegocioException.class);
		}
	}

	@Test
	void usaADuracaoConfigurada() {
		ConsultaService comUmaHora = criarService(60);
		when(repository.findAtivasDoProfissionalNaData(2L, DIA))
				.thenReturn(List.of(existente(drJoao, maria, DIA, "14:00", SituacaoConsulta.AGENDADA)));

		assertThatThrownBy(() -> comUmaHora.agendarRotina(rotina(DIA, "14:45")))
				.hasMessageEndingWith("(cada consulta dura 60 minutos).");
	}

	@Test
	void consideraConsultaDoDiaSeguinteQuandoOHorarioCruzaAMeiaNoite() {
		// lenient: o service consulta também o próprio dia, com outro argumento, antes do dia seguinte.
		lenient().when(repository.findAtivasDoProfissionalNaData(2L, DIA.plusDays(1)))
				.thenReturn(List.of(existente(drJoao, maria, DIA.plusDays(1), "00:10", SituacaoConsulta.AGENDADA)));

		assertThatThrownBy(() -> service.agendarRotina(rotina(DIA, "23:50")))
				.hasMessageStartingWith("O profissional Dr. João Souza já tem consulta em 16/01/2030 às 00:10");
	}

	@Test
	void consideraConsultaDoDiaAnteriorQuandoOHorarioEhLogoAposAMeiaNoite() {
		when(repository.findAtivasDoPacienteNaData(1L, DIA.minusDays(1)))
				.thenReturn(List.of(existente(cardiologista, maria, DIA.minusDays(1), "23:45", SituacaoConsulta.CONFIRMADA)));

		assertThatThrownBy(() -> service.agendarRotina(rotina(DIA, "00:05")))
				.hasMessageStartingWith("O paciente Maria Silva já tem consulta em 14/01/2030 às 23:45");
	}

	// Mudanças de situação -----------------------------------------------------------------------------

	private Consulta consultaSalva(SituacaoConsulta situacao) {
		Consulta consulta = existente(drJoao, maria, DIA, "14:00", situacao);
		when(repository.findById(10L)).thenReturn(Optional.of(consulta));
		return consulta;
	}

	@Test
	void confirma() {
		Consulta consulta = consultaSalva(SituacaoConsulta.AGENDADA);

		assertThat(service.confirmar(10L).situacao().codigo()).isEqualTo("CONFIRMADA");
		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.CONFIRMADA);
	}

	@Test
	void iniciaAtendimento() {
		consultaSalva(SituacaoConsulta.CONFIRMADA);

		assertThat(service.iniciarAtendimento(10L).situacao().codigo()).isEqualTo("EM_ATENDIMENTO");
	}

	@Test
	void naoIniciaAtendimentoSeOPacienteFoiInativado() {
		Consulta consulta = consultaSalva(SituacaoConsulta.CONFIRMADA);
		maria.inativar();

		assertThatThrownBy(() -> service.iniciarAtendimento(10L)).isInstanceOf(RegraNegocioException.class);
		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.CONFIRMADA);
	}

	@Test
	void conclui() {
		consultaSalva(SituacaoConsulta.EM_ATENDIMENTO);

		assertThat(service.concluir(10L).situacao().codigo()).isEqualTo("CONCLUIDA");
	}

	@Test
	void cancela() {
		consultaSalva(SituacaoConsulta.CONFIRMADA);

		assertThat(service.cancelar(10L).situacao().codigo()).isEqualTo("CANCELADA");
	}

	@Test
	void registraNaoComparecimento() {
		consultaSalva(SituacaoConsulta.AGENDADA);

		assertThat(service.registrarNaoComparecimento(10L).situacao().codigo()).isEqualTo("NAO_COMPARECEU");
	}

	@Test
	void registraObservacoesDuranteOAtendimento() {
		consultaSalva(SituacaoConsulta.EM_ATENDIMENTO);

		assertThat(service.registrarObservacoes(10L, new ObservacoesRequest(" Pressão 12x8. ")).observacoes())
				.isEqualTo("Pressão 12x8.");
	}

	@Test
	void naoRegistraObservacoesAntesDoAtendimento() {
		consultaSalva(SituacaoConsulta.AGENDADA);

		assertThatThrownBy(() -> service.registrarObservacoes(10L, new ObservacoesRequest("Texto")))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void transicaoInvalidaPropagaAExcecaoDaMaquinaDeEstados() {
		consultaSalva(SituacaoConsulta.AGENDADA);

		assertThatThrownBy(() -> service.concluir(10L))
				.isInstanceOf(TransicaoSituacaoInvalidaException.class)
				.hasMessage("Não é possível concluir uma consulta na situação Agendada.");
	}

	@Test
	void consultaInexistente() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.confirmar(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Consulta não encontrada (id 99).");
	}

	// Consultas ----------------------------------------------------------------------------------------

	@Test
	void buscaPorId() {
		consultaSalva(SituacaoConsulta.AGENDADA);

		assertThat(service.buscarPorId(10L).motivo()).isEqualTo("Retorno");
	}

	@Test
	void listaComOsFiltrosInformados() {
		when(repository.findComFiltros(SituacaoConsulta.CONFIRMADA, 2L, DIA))
				.thenReturn(List.of(existente(drJoao, maria, DIA, "14:00", SituacaoConsulta.CONFIRMADA)));

		assertThat(service.listar(SituacaoConsulta.CONFIRMADA, 2L, DIA)).hasSize(1);
	}

	@Test
	void agendaDoProfissional() {
		when(repository.findAgendaDoProfissional(2L, DIA)).thenReturn(List.of(
				existente(drJoao, maria, DIA, "09:00", SituacaoConsulta.AGENDADA),
				existente(drJoao, maria, DIA, "10:00", SituacaoConsulta.CONFIRMADA)));

		assertThat(service.agendaDoProfissional(2L, DIA)).extracting(ConsultaResponse::horario)
				.containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0));
	}

	@Test
	void agendaDeProfissionalInexistente() {
		assertThatThrownBy(() -> service.agendaDoProfissional(9L, DIA)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

}
