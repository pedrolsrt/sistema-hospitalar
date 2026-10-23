package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.ConclusaoProcedimentoRequest;
import br.pucminas.hospital.dto.InicioProcedimentoRequest;
import br.pucminas.hospital.dto.ObservacoesRequest;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoRequest;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoResponse;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import br.pucminas.hospital.mapper.ProcedimentoCirurgicoMapper;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.Cirurgiao;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.MedicoEspecialista;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.model.SituacaoProcedimento;
import br.pucminas.hospital.model.TipoCirurgia;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcedimentoCirurgicoServiceTest {

	private static final LocalDateTime PREVISTA = LocalDateTime.of(2030, 3, 10, 8, 0);

	@Mock
	private ProcedimentoCirurgicoRepository repository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private ProfissionalSaudeRepository profissionalRepository;

	private ProcedimentoCirurgicoService service;

	private Paciente maria;
	private Cirurgiao cirurgiao;

	@BeforeEach
	void configurar() {
		service = new ProcedimentoCirurgicoService(repository, pacienteRepository, profissionalRepository,
				new ProcedimentoCirurgicoMapper());
		maria = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
		cirurgiao = new Cirurgiao("Dra. Carla Mendes", "CRM-MG 3", AreaAtuacao.CIRURGIA_GERAL);
		lenient().when(pacienteRepository.findById(1L)).thenReturn(Optional.of(maria));
		lenient().when(profissionalRepository.findById(3L)).thenReturn(Optional.of(cirurgiao));
		lenient().when(repository.save(any(ProcedimentoCirurgico.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));
	}

	private static ProcedimentoCirurgicoRequest request(Long cirurgiaoId, String observacoesPre) {
		return new ProcedimentoCirurgicoRequest(1L, cirurgiaoId, "Apendicectomia", TipoCirurgia.URGENCIA, PREVISTA,
				observacoesPre);
	}

	private ProcedimentoCirurgico salvo() {
		ProcedimentoCirurgico procedimento = new ProcedimentoCirurgico(maria, cirurgiao, "Apendicectomia",
				TipoCirurgia.URGENCIA, PREVISTA);
		when(repository.findById(10L)).thenReturn(Optional.of(procedimento));
		return procedimento;
	}

	@Test
	void agendaComCirurgiaoEObservacoesPreOperatorias() {
		ProcedimentoCirurgicoResponse resposta = service.agendar(request(3L, " Jejum de 8 horas. "));

		verify(repository).save(any(ProcedimentoCirurgico.class));
		assertThat(resposta.situacao().codigo()).isEqualTo("AGENDADO");
		assertThat(resposta.tipoCirurgia().descricao()).isEqualTo("Urgência");
		assertThat(resposta.observacoesPreOperatorias()).isEqualTo("Jejum de 8 horas.");
	}

	@Test
	void naoAgendaComClinicoGeral() {
		when(profissionalRepository.findById(4L)).thenReturn(Optional.of(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1")));

		assertThatThrownBy(() -> service.agendar(request(4L, null)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O profissional Dra. Ana Lima não pode realizar procedimentos cirúrgicos: é necessário um cirurgião.");
		verify(repository, never()).save(any());
	}

	@Test
	void naoAgendaComEspecialistaMesmoDaAreaCirurgica() {
		when(profissionalRepository.findById(5L)).thenReturn(Optional.of(
				new MedicoEspecialista("Dr. João Souza", "CRM-MG 2", AreaAtuacao.CIRURGIA_GERAL)));

		assertThatThrownBy(() -> service.agendar(request(5L, null))).isInstanceOf(RegraNegocioException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void naoAgendaComCirurgiaoInativo() {
		cirurgiao.inativar();

		assertThatThrownBy(() -> service.agendar(request(3L, null)))
				.hasMessage("O profissional Dra. Carla Mendes está inativo e não pode realizar atendimentos.");
	}

	@Test
	void naoAgendaComReferenciaInexistente() {
		assertThatThrownBy(() -> service.agendar(request(9L, null))).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void iniciaEConclui() {
		salvo();

		ProcedimentoCirurgicoResponse iniciado = service.iniciar(10L, new InicioProcedimentoRequest(PREVISTA));
		ProcedimentoCirurgicoResponse concluido = service.concluir(10L,
				new ConclusaoProcedimentoRequest(PREVISTA.plusHours(2)));

		assertThat(iniciado.situacao().codigo()).isEqualTo("EM_ANDAMENTO");
		assertThat(concluido.situacao().codigo()).isEqualTo("CONCLUIDO");
		assertThat(concluido.dataHoraFim()).isEqualTo(PREVISTA.plusHours(2));
	}

	@Test
	void naoConcluiSemIniciar() {
		ProcedimentoCirurgico procedimento = salvo();

		assertThatThrownBy(() -> service.concluir(10L, new ConclusaoProcedimentoRequest(PREVISTA)))
				.isInstanceOf(TransicaoSituacaoInvalidaException.class)
				.hasMessage("Não é possível concluir um procedimento cirúrgico na situação Agendado.");
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.AGENDADO);
	}

	@Test
	void naoConcluiComFimAntesDoInicio() {
		ProcedimentoCirurgico procedimento = salvo();
		procedimento.iniciar(PREVISTA);

		assertThatThrownBy(() -> service.concluir(10L, new ConclusaoProcedimentoRequest(PREVISTA.minusMinutes(1))))
				.isInstanceOf(DataInvalidaException.class);
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.EM_ANDAMENTO);
	}

	@Test
	void naoIniciaSeOCirurgiaoFoiInativado() {
		salvo();
		cirurgiao.inativar();

		assertThatThrownBy(() -> service.iniciar(10L, new InicioProcedimentoRequest(PREVISTA)))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void cancelaAgendadoMasNaoEmAndamento() {
		salvo();
		assertThat(service.cancelar(10L).situacao().codigo()).isEqualTo("CANCELADO");

		ProcedimentoCirurgico emAndamento = salvo();
		emAndamento.iniciar(PREVISTA);
		assertThatThrownBy(() -> service.cancelar(10L)).isInstanceOf(TransicaoSituacaoInvalidaException.class);
	}

	@Test
	void registraObservacoesPreEPosOperatorias() {
		ProcedimentoCirurgico procedimento = salvo();

		service.registrarObservacoesPreOperatorias(10L, new ObservacoesRequest("Jejum."));
		procedimento.iniciar(PREVISTA);
		ProcedimentoCirurgicoResponse resposta = service.registrarObservacoesPosOperatorias(10L,
				new ObservacoesRequest("Sem intercorrências."));

		assertThat(resposta.observacoesPreOperatorias()).isEqualTo("Jejum.");
		assertThat(resposta.observacoesPosOperatorias()).isEqualTo("Sem intercorrências.");
	}

	@Test
	void naoRegistraObservacoesPosOperatoriasAntesDoInicio() {
		salvo();

		assertThatThrownBy(() -> service.registrarObservacoesPosOperatorias(10L, new ObservacoesRequest("Texto")))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void procedimentoInexistente() {
		assertThatThrownBy(() -> service.buscarPorId(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Procedimento cirúrgico não encontrado (id 99).");
	}

	@Test
	void listaPorDataERealizadosPorCirurgiao() {
		ProcedimentoCirurgico procedimento = new ProcedimentoCirurgico(maria, cirurgiao, "Apendicectomia",
				TipoCirurgia.URGENCIA, PREVISTA);
		when(repository.findPrevistosNaData(PREVISTA.toLocalDate())).thenReturn(List.of(procedimento));
		when(repository.findConcluidosDoCirurgiao(3L)).thenReturn(List.of(procedimento));

		assertThat(service.listarPorData(PREVISTA.toLocalDate())).hasSize(1);
		assertThat(service.listarRealizadosPorCirurgiao(3L)).hasSize(1);
	}

	@Test
	void realizadosDeCirurgiaoInexistente() {
		assertThatThrownBy(() -> service.listarRealizadosPorCirurgiao(9L))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

}
