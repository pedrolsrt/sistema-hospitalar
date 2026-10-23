package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.AtendimentoEmergenciaRequest;
import br.pucminas.hospital.dto.AtendimentoEmergenciaResponse;
import br.pucminas.hospital.dto.PrioridadeRequest;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.mapper.AtendimentoEmergenciaMapper;
import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.PrioridadeEmergencia;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.SituacaoEmergencia;
import br.pucminas.hospital.repository.AtendimentoEmergenciaRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtendimentoEmergenciaServiceTest {

	private static final LocalDateTime AGORA = LocalDateTime.of(2030, 1, 15, 22, 40);

	@Mock
	private AtendimentoEmergenciaRepository repository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private ProfissionalSaudeRepository profissionalRepository;

	private AtendimentoEmergenciaService service;

	private Paciente maria;
	private ProfissionalSaude plantonista;

	@BeforeEach
	void configurar() {
		service = new AtendimentoEmergenciaService(repository, pacienteRepository, profissionalRepository,
				new AtendimentoEmergenciaMapper(), Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
		maria = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
		plantonista = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		lenient().when(pacienteRepository.findById(1L)).thenReturn(Optional.of(maria));
		lenient().when(profissionalRepository.findById(2L)).thenReturn(Optional.of(plantonista));
		lenient().when(repository.save(any(AtendimentoEmergencia.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));
	}

	private AtendimentoEmergencia salva(PrioridadeEmergencia prioridade) {
		AtendimentoEmergencia emergencia = new AtendimentoEmergencia(maria, plantonista, prioridade, AGORA);
		when(repository.findById(10L)).thenReturn(Optional.of(emergencia));
		return emergencia;
	}

	@Test
	void registraChegadaAguardandoAvaliacao() {
		LocalDateTime chegada = AGORA.minusMinutes(5);

		AtendimentoEmergenciaResponse resposta = service.registrarChegada(
				new AtendimentoEmergenciaRequest(1L, 2L, PrioridadeEmergencia.ALTA, chegada));

		assertThat(resposta.situacao().codigo()).isEqualTo("AGUARDANDO_AVALIACAO");
		assertThat(resposta.prioridade().descricao()).isEqualTo("Alta");
		assertThat(resposta.dataHoraChegada()).isEqualTo(chegada);
		assertThat(resposta.profissional().nome()).isEqualTo("Dra. Ana Lima");
	}

	@Test
	void semChegadaInformadaUsaOMomentoDoRegistroESemPrioridadeAguardaTriagem() {
		AtendimentoEmergenciaResponse resposta = service.registrarChegada(
				new AtendimentoEmergenciaRequest(1L, 2L, null, null));

		assertThat(resposta.dataHoraChegada()).isEqualTo(AGORA);
		assertThat(resposta.prioridade()).isNull();
	}

	@Test
	void naoRegistraChegadaDePacienteInativo() {
		maria.inativar();

		assertThatThrownBy(() -> service.registrarChegada(new AtendimentoEmergenciaRequest(1L, 2L, null, null)))
				.hasMessage("O paciente Maria Silva está inativo e não pode ser atendido.");
		verify(repository, never()).save(any());
	}

	@Test
	void naoRegistraComPlantonistaInativo() {
		plantonista.inativar();

		assertThatThrownBy(() -> service.registrarChegada(new AtendimentoEmergenciaRequest(1L, 2L, null, null)))
				.isInstanceOf(RegraNegocioException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void naoRegistraComReferenciaInexistente() {
		assertThatThrownBy(() -> service.registrarChegada(new AtendimentoEmergenciaRequest(9L, 2L, null, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void defineEReclassificaAPrioridade() {
		AtendimentoEmergencia emergencia = salva(null);

		service.definirPrioridade(10L, new PrioridadeRequest(PrioridadeEmergencia.MODERADA));
		service.iniciar(10L);
		AtendimentoEmergenciaResponse resposta = service.definirPrioridade(10L,
				new PrioridadeRequest(PrioridadeEmergencia.CRITICA));

		assertThat(resposta.prioridade().codigo()).isEqualTo("CRITICA");
		assertThat(emergencia.getSituacao()).isEqualTo(SituacaoEmergencia.EM_ATENDIMENTO);
	}

	@Test
	void naoReclassificaAtendimentoConcluido() {
		AtendimentoEmergencia emergencia = salva(PrioridadeEmergencia.ALTA);
		emergencia.iniciar();
		emergencia.concluir();

		assertThatThrownBy(() -> service.definirPrioridade(10L, new PrioridadeRequest(PrioridadeEmergencia.BAIXA)))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void iniciaEConclui() {
		salva(PrioridadeEmergencia.ALTA);

		assertThat(service.iniciar(10L).situacao().codigo()).isEqualTo("EM_ATENDIMENTO");
		assertThat(service.concluir(10L).situacao().codigo()).isEqualTo("CONCLUIDO");
	}

	@Test
	void naoIniciaSemPrioridade() {
		AtendimentoEmergencia emergencia = salva(null);

		assertThatThrownBy(() -> service.iniciar(10L))
				.hasMessage("Defina a prioridade do atendimento de emergência antes de iniciar o atendimento.");
		assertThat(emergencia.getSituacao()).isEqualTo(SituacaoEmergencia.AGUARDANDO_AVALIACAO);
	}

	@Test
	void naoConcluiSemIniciar() {
		salva(PrioridadeEmergencia.ALTA);

		assertThatThrownBy(() -> service.concluir(10L)).isInstanceOf(TransicaoSituacaoInvalidaException.class);
	}

	@Test
	void atendimentoInexistente() {
		assertThatThrownBy(() -> service.buscarPorId(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Atendimento de emergência não encontrado (id 99).");
	}

	@Test
	void filaEListagemPorSituacao() {
		AtendimentoEmergencia critico = new AtendimentoEmergencia(maria, plantonista, PrioridadeEmergencia.CRITICA, AGORA);
		AtendimentoEmergencia baixo = new AtendimentoEmergencia(maria, plantonista, PrioridadeEmergencia.BAIXA, AGORA);
		when(repository.findFilaDeAvaliacao()).thenReturn(List.of(critico, baixo));
		when(repository.findBySituacaoOrderByDataHoraChegadaAsc(SituacaoEmergencia.AGUARDANDO_AVALIACAO))
				.thenReturn(List.of(baixo, critico));

		assertThat(service.filaDeAvaliacao()).extracting(resposta -> resposta.prioridade().codigo())
				.containsExactly("CRITICA", "BAIXA");
		assertThat(service.listarPorSituacao(SituacaoEmergencia.AGUARDANDO_AVALIACAO)).hasSize(2);
	}

}
