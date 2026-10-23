package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.AltaRequest;
import br.pucminas.hospital.dto.InternacaoRequest;
import br.pucminas.hospital.dto.InternacaoResponse;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.QuartoSemVagaException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.InternacaoMapper;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.Internacao;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
import br.pucminas.hospital.repository.InternacaoRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import br.pucminas.hospital.repository.QuartoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InternacaoServiceTest {

	private static final LocalDate ENTRADA = LocalDate.of(2030, 2, 1);

	@Mock
	private InternacaoRepository repository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private ProfissionalSaudeRepository profissionalRepository;

	@Mock
	private QuartoRepository quartoRepository;

	private InternacaoService service;

	private Paciente maria;
	private ProfissionalSaude medico;
	private Quarto quarto;

	@BeforeEach
	void configurar() {
		service = new InternacaoService(repository, pacienteRepository, profissionalRepository, quartoRepository,
				new InternacaoMapper());
		maria = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
		ReflectionTestUtils.setField(maria, "id", 1L);
		medico = new ClinicoGeral("Dr. João Souza", "CRM-MG 1");
		quarto = new Quarto("201", 2, TipoQuarto.APARTAMENTO, 2);
		lenient().when(pacienteRepository.findById(1L)).thenReturn(Optional.of(maria));
		lenient().when(profissionalRepository.findById(2L)).thenReturn(Optional.of(medico));
		lenient().when(quartoRepository.findById(3L)).thenReturn(Optional.of(quarto));
		lenient().when(repository.save(any(Internacao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
	}

	private static InternacaoRequest request() {
		return new InternacaoRequest(1L, 2L, 3L, ENTRADA, ENTRADA.plusDays(5));
	}

	@Test
	void internaEOcupaUmaVagaDoQuarto() {
		InternacaoResponse resposta = service.internar(request());

		verify(repository).save(any(Internacao.class));
		assertThat(quarto.getOcupacaoAtual()).isEqualTo(1);
		assertThat(resposta.ativa()).isTrue();
		assertThat(resposta.quarto().numero()).isEqualTo("201");
		assertThat(resposta.quarto().tipo().descricao()).isEqualTo("Apartamento");
	}

	@Test
	void naoInternaPacienteJaInternado() {
		Quarto outro = new Quarto("105", 1, TipoQuarto.ENFERMARIA, 4);
		Internacao ativa = new Internacao(maria, medico, outro, ENTRADA.minusDays(3), ENTRADA.plusDays(2));
		when(repository.findByPacienteIdAndDataEfetivaAltaIsNull(1L)).thenReturn(Optional.of(ativa));

		assertThatThrownBy(() -> service.internar(request()))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O paciente Maria Silva já está internado no quarto 105 desde 29/01/2030.");
		assertThat(quarto.getOcupacaoAtual()).isZero();
		verify(repository, never()).save(any());
	}

	@Test
	void naoInternaEmQuartoSemVaga() {
		quarto.ocupar();
		quarto.ocupar();

		assertThatThrownBy(() -> service.internar(request())).isInstanceOf(QuartoSemVagaException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void naoInternaPacienteInativo() {
		maria.inativar();

		assertThatThrownBy(() -> service.internar(request()))
				.hasMessage("O paciente Maria Silva está inativo e não pode ser atendido.");
		assertThat(quarto.getOcupacaoAtual()).isZero();
	}

	@Test
	void naoInternaComAltaPrevistaAntesDaEntrada() {
		assertThatThrownBy(() -> service.internar(new InternacaoRequest(1L, 2L, 3L, ENTRADA, ENTRADA.minusDays(1))))
				.isInstanceOf(DataInvalidaException.class);
		assertThat(quarto.getOcupacaoAtual()).isZero();
	}

	@Test
	void naoInternaComReferenciaInexistente() {
		assertThatThrownBy(() -> service.internar(new InternacaoRequest(9L, 2L, 3L, ENTRADA, ENTRADA)))
				.hasMessage("Paciente não encontrado (id 9).");
		assertThatThrownBy(() -> service.internar(new InternacaoRequest(1L, 9L, 3L, ENTRADA, ENTRADA)))
				.hasMessage("Profissional de saúde não encontrado (id 9).");
		assertThatThrownBy(() -> service.internar(new InternacaoRequest(1L, 2L, 9L, ENTRADA, ENTRADA)))
				.hasMessage("Quarto não encontrado (id 9).");
	}

	@Test
	void registraAltaELiberaAVaga() {
		Internacao internacao = new Internacao(maria, medico, quarto, ENTRADA, ENTRADA.plusDays(5));
		when(repository.findById(10L)).thenReturn(Optional.of(internacao));

		InternacaoResponse resposta = service.registrarAlta(10L, new AltaRequest(ENTRADA.plusDays(4)));

		assertThat(resposta.ativa()).isFalse();
		assertThat(resposta.dataEfetivaAlta()).isEqualTo(ENTRADA.plusDays(4));
		assertThat(quarto.getOcupacaoAtual()).isZero();
	}

	@Test
	void naoRegistraAltaDuasVezes() {
		Internacao internacao = new Internacao(maria, medico, quarto, ENTRADA, ENTRADA.plusDays(5));
		internacao.darAlta(ENTRADA.plusDays(1));
		when(repository.findById(10L)).thenReturn(Optional.of(internacao));

		assertThatThrownBy(() -> service.registrarAlta(10L, new AltaRequest(ENTRADA.plusDays(2))))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageStartingWith("Esta internação já foi encerrada");
	}

	@Test
	void naoRegistraAltaAntesDaEntrada() {
		Internacao internacao = new Internacao(maria, medico, quarto, ENTRADA, ENTRADA.plusDays(5));
		when(repository.findById(10L)).thenReturn(Optional.of(internacao));

		assertThatThrownBy(() -> service.registrarAlta(10L, new AltaRequest(ENTRADA.minusDays(1))))
				.isInstanceOf(DataInvalidaException.class);
		assertThat(internacao.isAtiva()).isTrue();
	}

	@Test
	void internacaoInexistente() {
		assertThatThrownBy(() -> service.buscarPorId(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Internação não encontrada (id 99).");
	}

	@Test
	void listaAtivasEEncerradas() {
		Internacao ativa = new Internacao(maria, medico, quarto, ENTRADA, ENTRADA.plusDays(5));
		Internacao encerrada = new Internacao(maria, medico, quarto, ENTRADA.minusDays(20), ENTRADA.minusDays(15));
		encerrada.darAlta(ENTRADA.minusDays(15));
		when(repository.findByDataEfetivaAltaIsNullOrderByDataEntradaAsc()).thenReturn(List.of(ativa));
		when(repository.findByDataEfetivaAltaIsNotNullOrderByDataEfetivaAltaDesc()).thenReturn(List.of(encerrada));

		assertThat(service.listarAtivas()).extracting(InternacaoResponse::ativa).containsExactly(true);
		assertThat(service.listarEncerradas()).extracting(InternacaoResponse::ativa).containsExactly(false);
	}

}
