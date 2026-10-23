package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.ProfissionalSaudeAtualizacaoRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeRequest;
import br.pucminas.hospital.dto.ProfissionalSaudeResponse;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.ProfissionalSaudeMapper;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.CategoriaProfissional;
import br.pucminas.hospital.model.Cirurgiao;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.MedicoEspecialista;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.repository.AtendimentoEmergenciaRepository;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.InternacaoRepository;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ProfissionalSaudeServiceTest {

	@Mock
	private ProfissionalSaudeRepository repository;

	@Mock
	private ConsultaRepository consultaRepository;

	@Mock
	private AtendimentoEmergenciaRepository emergenciaRepository;

	@Mock
	private ProcedimentoCirurgicoRepository procedimentoRepository;

	@Mock
	private InternacaoRepository internacaoRepository;

	private ProfissionalSaudeService service;

	@BeforeEach
	void criarService() {
		service = new ProfissionalSaudeService(repository, consultaRepository, emergenciaRepository,
				procedimentoRepository, internacaoRepository, new ProfissionalSaudeMapper());
	}

	private static ProfissionalSaudeRequest request(CategoriaProfissional categoria, AreaAtuacao area) {
		return new ProfissionalSaudeRequest("Dra. Ana Lima", "CRM-MG 12345", categoria, area, null,
				"ana@hospital.com");
	}

	static Stream<Arguments> categorias() {
		return Stream.of(
				Arguments.of(CategoriaProfissional.CLINICO_GERAL, null, ClinicoGeral.class, null),
				Arguments.of(CategoriaProfissional.MEDICO_ESPECIALISTA, AreaAtuacao.CARDIOLOGIA,
						MedicoEspecialista.class, new CodigoDescricao("CARDIOLOGIA", "Cardiologia")),
				Arguments.of(CategoriaProfissional.CIRURGIAO, AreaAtuacao.CIRURGIA_GERAL, Cirurgiao.class,
						new CodigoDescricao("CIRURGIA_GERAL", "Cirurgia Geral")));
	}

	@ParameterizedTest
	@MethodSource("categorias")
	void cadastraOProfissionalDaCategoria(CategoriaProfissional categoria, AreaAtuacao area,
			Class<? extends ProfissionalSaude> classeEsperada, CodigoDescricao areaEsperada) {
		when(repository.existsByRegistroProfissional("CRM-MG 12345")).thenReturn(false);
		when(repository.save(any(ProfissionalSaude.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		ProfissionalSaudeResponse resposta = service.cadastrar(request(categoria, area));

		ArgumentCaptor<ProfissionalSaude> salvo = ArgumentCaptor.forClass(ProfissionalSaude.class);
		verify(repository).save(salvo.capture());
		assertThat(salvo.getValue()).isExactlyInstanceOf(classeEsperada);
		assertThat(resposta.categoria()).isEqualTo(new CodigoDescricao(categoria.name(), categoria.getDescricao()));
		assertThat(resposta.areaAtuacao()).isEqualTo(areaEsperada);
		assertThat(resposta.email()).isEqualTo("ana@hospital.com");
	}

	@Test
	void naoCadastraRegistroJaExistente() {
		when(repository.existsByRegistroProfissional("CRM-MG 12345")).thenReturn(true);

		assertThatThrownBy(() -> service.cadastrar(request(CategoriaProfissional.CLINICO_GERAL, null)))
				.isInstanceOf(DadoJaCadastradoException.class)
				.hasMessage("Já existe um profissional cadastrado com o registro CRM-MG 12345.");
		verify(repository, never()).save(any());
	}

	@Test
	void atualizaApenasOsDadosComuns() {
		MedicoEspecialista especialista = new MedicoEspecialista("Dr. João", "CRM-MG 2", AreaAtuacao.NEUROLOGIA);
		when(repository.findById(2L)).thenReturn(Optional.of(especialista));

		ProfissionalSaudeResponse resposta = service.atualizar(2L,
				new ProfissionalSaudeAtualizacaoRequest("Dr. João Souza", "3133334444", "joao@hospital.com"));

		assertThat(especialista.getNome()).isEqualTo("Dr. João Souza");
		assertThat(especialista.getTelefone()).isEqualTo("3133334444");
		assertThat(resposta.registroProfissional()).isEqualTo("CRM-MG 2");
		assertThat(resposta.areaAtuacao().codigo()).isEqualTo("NEUROLOGIA");
	}

	@Test
	void naoAtualizaProfissionalInexistente() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.atualizar(9L, new ProfissionalSaudeAtualizacaoRequest("Nome", null, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Profissional de saúde não encontrado (id 9).");
	}

	@Test
	void buscaPorId() {
		when(repository.findById(1L)).thenReturn(Optional.of(new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1")));

		assertThat(service.buscarPorId(1L).categoria().descricao()).isEqualTo("Clínico geral");
	}

	@Test
	void buscaPorIdInexistente() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarPorId(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void listaTodosOuSoAtivos() {
		ProfissionalSaude ativo = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		ProfissionalSaude inativo = new ClinicoGeral("Dr. Bruno Reis", "CRM-MG 2");
		inativo.inativar();
		when(repository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(ativo));
		when(repository.findAll(Sort.by("nome"))).thenReturn(List.of(ativo, inativo));

		assertThat(service.listar(true)).hasSize(1);
		assertThat(service.listar(false)).extracting(ProfissionalSaudeResponse::ativo).containsExactly(true, false);
	}

	@Test
	void listaPorArea() {
		when(repository.findAtivosPorArea(AreaAtuacao.CARDIOLOGIA)).thenReturn(List.of(
				new Cirurgiao("Dra. Carla", "CRM-MG 3", AreaAtuacao.CARDIOLOGIA),
				new MedicoEspecialista("Dr. João", "CRM-MG 2", AreaAtuacao.CARDIOLOGIA)));

		assertThat(service.listarPorArea(AreaAtuacao.CARDIOLOGIA))
				.extracting(resposta -> resposta.categoria().codigo())
				.containsExactly("CIRURGIAO", "MEDICO_ESPECIALISTA");
	}

	@Test
	void inativaEReativa() {
		ProfissionalSaude profissional = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		when(repository.findById(1L)).thenReturn(Optional.of(profissional));

		assertThat(service.inativar(1L).ativo()).isFalse();
		assertThat(service.inativar(1L).ativo()).isFalse();
		assertThat(service.reativar(1L).ativo()).isTrue();
		assertThat(profissional.isAtivo()).isTrue();
	}

	@Test
	void naoInativaProfissionalComConsultaAtiva() {
		ProfissionalSaude profissional = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		when(repository.findById(1L)).thenReturn(Optional.of(profissional));
		when(consultaRepository.existeAtivaDoProfissional(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O profissional Dra. Ana Lima não pode ser inativado porque tem sob sua responsabilidade: "
						+ "consulta agendada, confirmada ou em atendimento.");
		assertThat(profissional.isAtivo()).isTrue();
	}

	@Test
	void naoInativaProfissionalComEmergenciaAtiva() {
		ProfissionalSaude profissional = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		when(repository.findById(1L)).thenReturn(Optional.of(profissional));
		when(emergenciaRepository.existeAtivoDoProfissional(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L)).hasMessageContaining("atendimento de emergência");
		assertThat(profissional.isAtivo()).isTrue();
	}

	@Test
	void naoInativaCirurgiaoComProcedimentoAtivo() {
		ProfissionalSaude cirurgiao = new Cirurgiao("Dra. Carla", "CRM-MG 3", AreaAtuacao.CIRURGIA_GERAL);
		when(repository.findById(1L)).thenReturn(Optional.of(cirurgiao));
		when(procedimentoRepository.existeAtivoDoProfissional(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L)).hasMessageContaining("procedimento cirúrgico");
		assertThat(cirurgiao.isAtivo()).isTrue();
	}

	@Test
	void naoInativaProfissionalResponsavelPorInternacaoAtiva() {
		ProfissionalSaude profissional = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		when(repository.findById(1L)).thenReturn(Optional.of(profissional));
		when(internacaoRepository.existsByProfissionalResponsavelIdAndDataEfetivaAltaIsNull(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(1L)).hasMessageEndingWith(": internação ativa.");
		assertThat(profissional.isAtivo()).isTrue();
	}

	@Test
	void naoInativaNemReativaProfissionalInexistente() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.inativar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
		assertThatThrownBy(() -> service.reativar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

}
