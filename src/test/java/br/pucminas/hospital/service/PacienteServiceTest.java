package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.EnderecoDto;
import br.pucminas.hospital.dto.PacienteAtualizacaoRequest;
import br.pucminas.hospital.dto.PacienteRequest;
import br.pucminas.hospital.dto.PacienteResponse;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.PacienteMapper;
import br.pucminas.hospital.model.Endereco;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.repository.AtendimentoEmergenciaRepository;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.InternacaoRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

	private static final LocalDate NASCIMENTO = LocalDate.of(1990, 5, 10);

	@Mock
	private PacienteRepository repository;

	@Mock
	private InternacaoRepository internacaoRepository;

	@Mock
	private ConsultaRepository consultaRepository;

	@Mock
	private AtendimentoEmergenciaRepository emergenciaRepository;

	@Mock
	private ProcedimentoCirurgicoRepository procedimentoRepository;

	private PacienteService service;

	@BeforeEach
	void criarService() {
		service = new PacienteService(repository, internacaoRepository, consultaRepository, emergenciaRepository,
				procedimentoRepository, new PacienteMapper());
	}

	private PacienteRequest request(String cpf) {
		return new PacienteRequest(" Maria Silva ", cpf, NASCIMENTO, "(31) 99999-0000", "maria@email.com",
				new EnderecoDto("Rua A", "10", " ", "Centro", "Belo Horizonte", "mg", "30140-071"));
	}

	private Paciente pacienteSalvo() {
		return new Paciente("Maria Silva", "52998224725", NASCIMENTO);
	}

	@Test
	void cadastraPacienteComCpfECepSoComDigitos() {
		when(repository.existsByCpf("52998224725")).thenReturn(false);
		when(repository.save(any(Paciente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		PacienteResponse resposta = service.cadastrar(request("529.982.247-25"));

		ArgumentCaptor<Paciente> salvo = ArgumentCaptor.forClass(Paciente.class);
		verify(repository).save(salvo.capture());
		assertThat(salvo.getValue().getCpf()).isEqualTo("52998224725");
		assertThat(salvo.getValue().getNome()).isEqualTo("Maria Silva");
		assertThat(salvo.getValue().isAtivo()).isTrue();
		assertThat(salvo.getValue().getEndereco())
				.isEqualTo(new Endereco("Rua A", "10", null, "Centro", "Belo Horizonte", "MG", "30140071"));
		assertThat(resposta.cpf()).isEqualTo("52998224725");
		assertThat(resposta.endereco().uf()).isEqualTo("MG");
	}

	@Test
	void naoCadastraCpfJaExistente() {
		when(repository.existsByCpf("52998224725")).thenReturn(true);

		assertThatThrownBy(() -> service.cadastrar(request("52998224725")))
				.isInstanceOf(DadoJaCadastradoException.class)
				.hasMessage("Já existe um paciente cadastrado com o CPF 529.982.247-25.");
		verify(repository, never()).save(any());
	}

	@Test
	void cadastraPacienteSemEndereco() {
		when(repository.save(any(Paciente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		PacienteResponse resposta = service.cadastrar(
				new PacienteRequest("Maria Silva", "52998224725", NASCIMENTO, "", null, null));

		assertThat(resposta.endereco()).isNull();
		assertThat(resposta.telefone()).isNull();
	}

	@Test
	void atualizaDadosSemMudarOCpf() {
		Paciente paciente = pacienteSalvo();
		when(repository.findById(1L)).thenReturn(Optional.of(paciente));

		PacienteResponse resposta = service.atualizar(1L, new PacienteAtualizacaoRequest("Maria Souza",
				NASCIMENTO.minusDays(1), "3133334444", "maria.souza@email.com", null));

		assertThat(paciente.getNome()).isEqualTo("Maria Souza");
		assertThat(paciente.getDataNascimento()).isEqualTo(NASCIMENTO.minusDays(1));
		assertThat(paciente.getEmail()).isEqualTo("maria.souza@email.com");
		assertThat(resposta.cpf()).isEqualTo("52998224725");
	}

	@Test
	void naoAtualizaPacienteInexistente() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.atualizar(9L,
				new PacienteAtualizacaoRequest("Maria", NASCIMENTO, null, null, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Paciente não encontrado (id 9).");
	}

	@Test
	void buscaPorId() {
		when(repository.findById(1L)).thenReturn(Optional.of(pacienteSalvo()));

		assertThat(service.buscarPorId(1L).nome()).isEqualTo("Maria Silva");
	}

	@Test
	void buscaPorIdInexistente() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarPorId(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void listaSoAtivos() {
		when(repository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(pacienteSalvo()));

		assertThat(service.listar(true)).extracting(PacienteResponse::nome).containsExactly("Maria Silva");
	}

	@Test
	void listaTodosPorNome() {
		Paciente inativo = new Paciente("Ana Costa", "11144477735", NASCIMENTO);
		inativo.inativar();
		when(repository.findAll(Sort.by("nome"))).thenReturn(List.of(inativo, pacienteSalvo()));

		assertThat(service.listar(false)).extracting(PacienteResponse::ativo).containsExactly(false, true);
	}

	@Test
	void inativaEReativa() {
		Paciente paciente = pacienteSalvo();
		when(repository.findById(1L)).thenReturn(Optional.of(paciente));

		assertThat(service.inativar(1L).ativo()).isFalse();
		assertThat(paciente.isAtivo()).isFalse();
		assertThat(service.reativar(1L).ativo()).isTrue();
		assertThat(paciente.isAtivo()).isTrue();
	}

	@Test
	void naoInativaPacienteComInternacaoAtiva() {
		Paciente paciente = pacienteSalvo();
		when(repository.findById(1L)).thenReturn(Optional.of(paciente));
		when(internacaoRepository.existsByPacienteIdAndDataEfetivaAltaIsNull(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O paciente Maria Silva não pode ser inativado porque tem: internação ativa.");
		assertThat(paciente.isAtivo()).isTrue();
	}

	@Test
	void naoInativaPacienteComConsultaAtiva() {
		Paciente paciente = pacienteSalvo();
		when(repository.findById(1L)).thenReturn(Optional.of(paciente));
		when(consultaRepository.existeAtivaDoPaciente(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L))
				.hasMessage("O paciente Maria Silva não pode ser inativado porque tem: "
						+ "consulta agendada, confirmada ou em atendimento.");
		assertThat(paciente.isAtivo()).isTrue();
	}

	@Test
	void naoInativaPacienteComEmergenciaAtiva() {
		Paciente paciente = pacienteSalvo();
		when(repository.findById(1L)).thenReturn(Optional.of(paciente));
		when(emergenciaRepository.existeAtivoDoPaciente(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L)).hasMessageContaining("atendimento de emergência");
		assertThat(paciente.isAtivo()).isTrue();
	}

	@Test
	void naoInativaPacienteComProcedimentoAtivo() {
		Paciente paciente = pacienteSalvo();
		when(repository.findById(1L)).thenReturn(Optional.of(paciente));
		when(procedimentoRepository.existeAtivoDoPaciente(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L)).hasMessageContaining("procedimento cirúrgico agendado ou em andamento");
		assertThat(paciente.isAtivo()).isTrue();
	}

	@Test
	void mensagemListaTodosOsImpedimentos() {
		when(repository.findById(1L)).thenReturn(Optional.of(pacienteSalvo()));
		when(internacaoRepository.existsByPacienteIdAndDataEfetivaAltaIsNull(1L)).thenReturn(true);
		when(consultaRepository.existeAtivaDoPaciente(1L)).thenReturn(true);
		when(emergenciaRepository.existeAtivoDoPaciente(1L)).thenReturn(true);
		when(procedimentoRepository.existeAtivoDoPaciente(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L)).hasMessage("O paciente Maria Silva não pode ser inativado "
				+ "porque tem: internação ativa; consulta agendada, confirmada ou em atendimento; atendimento de "
				+ "emergência aguardando avaliação ou em atendimento; procedimento cirúrgico agendado ou em andamento.");
	}

	@Test
	void inativarPacienteJaInativoNaoTemEfeito() {
		Paciente paciente = pacienteSalvo();
		paciente.inativar();
		when(repository.findById(1L)).thenReturn(Optional.of(paciente));

		assertThat(service.inativar(1L).ativo()).isFalse();
	}

	@Test
	void naoInativaNemReativaPacienteInexistente() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.inativar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
		assertThatThrownBy(() -> service.reativar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

}
