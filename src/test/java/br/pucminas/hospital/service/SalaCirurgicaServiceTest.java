package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.SalaCirurgicaRequest;
import br.pucminas.hospital.dto.SalaCirurgicaResponse;
import br.pucminas.hospital.exception.DadoJaCadastradoException;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.mapper.SalaCirurgicaMapper;
import br.pucminas.hospital.model.SalaCirurgica;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import br.pucminas.hospital.repository.SalaCirurgicaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class SalaCirurgicaServiceTest {

	@Mock
	private SalaCirurgicaRepository repository;

	@Mock
	private ProcedimentoCirurgicoRepository procedimentoRepository;

	private SalaCirurgicaService service;

	@BeforeEach
	void criarService() {
		service = new SalaCirurgicaService(repository, procedimentoRepository, new SalaCirurgicaMapper());
	}

	private SalaCirurgica salva(String identificacao) {
		SalaCirurgica sala = new SalaCirurgica(identificacao);
		when(repository.findById(5L)).thenReturn(Optional.of(sala));
		return sala;
	}

	@Test
	void cadastraSalaAtiva() {
		when(repository.findByIdentificacao("Sala 1")).thenReturn(Optional.empty());
		when(repository.save(any(SalaCirurgica.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		SalaCirurgicaResponse resposta = service.cadastrar(new SalaCirurgicaRequest(" Sala 1 "));

		assertThat(resposta.identificacao()).isEqualTo("Sala 1");
		assertThat(resposta.ativa()).isTrue();
	}

	@Test
	void naoCadastraIdentificacaoJaExistente() {
		when(repository.findByIdentificacao("Sala 1")).thenReturn(Optional.of(new SalaCirurgica("Sala 1")));

		assertThatThrownBy(() -> service.cadastrar(new SalaCirurgicaRequest("Sala 1")))
				.isInstanceOf(DadoJaCadastradoException.class)
				.hasMessage("Já existe uma sala cirúrgica com a identificação Sala 1.");
		verify(repository, never()).save(any());
	}

	@Test
	void atualizaMantendoAPropriaIdentificacao() {
		SalaCirurgica sala = salva("Sala 1");
		when(repository.findByIdentificacao("Sala 1")).thenReturn(Optional.of(sala));

		assertThat(service.atualizar(5L, new SalaCirurgicaRequest("Sala 1")).identificacao()).isEqualTo("Sala 1");
	}

	@Test
	void naoAtualizaParaIdentificacaoDeOutraSala() {
		salva("Sala 1");
		when(repository.findByIdentificacao("Sala 2")).thenReturn(Optional.of(new SalaCirurgica("Sala 2")));

		assertThatThrownBy(() -> service.atualizar(5L, new SalaCirurgicaRequest("Sala 2")))
				.isInstanceOf(DadoJaCadastradoException.class);
	}

	@Test
	void listaTodasOuSoAsAtivas() {
		when(repository.findAll(Sort.by("identificacao")))
				.thenReturn(List.of(new SalaCirurgica("Sala 1"), new SalaCirurgica("Sala 2")));
		when(repository.findByAtivaTrueOrderByIdentificacaoAsc()).thenReturn(List.of(new SalaCirurgica("Sala 1")));

		assertThat(service.listar(false)).hasSize(2);
		assertThat(service.listar(true)).extracting(SalaCirurgicaResponse::identificacao).containsExactly("Sala 1");
	}

	@Test
	void inativaSalaSemProcedimentoAtivo() {
		salva("Sala 1");
		when(procedimentoRepository.existeAtivoNaSala(5L)).thenReturn(false);

		assertThat(service.inativar(5L).ativa()).isFalse();
	}

	@Test
	void naoInativaSalaComProcedimentoAgendadoOuEmAndamento() {
		SalaCirurgica sala = salva("Sala 1");
		when(procedimentoRepository.existeAtivoNaSala(5L)).thenReturn(true);

		assertThatThrownBy(() -> service.inativar(5L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("A sala cirúrgica Sala 1 não pode ser inativada porque tem procedimento cirúrgico "
						+ "agendado ou em andamento.");
		assertThat(sala.isAtiva()).isTrue();
	}

	@Test
	void inativarSalaJaInativaNaoTemEfeito() {
		salva("Sala 1").inativar();

		assertThat(service.inativar(5L).ativa()).isFalse();
		verify(procedimentoRepository, never()).existeAtivoNaSala(anyLong());
	}

	@Test
	void reativaSala() {
		salva("Sala 1").inativar();

		assertThat(service.reativar(5L).ativa()).isTrue();
	}

	@Test
	void salaInexistente() {
		assertThatThrownBy(() -> service.buscarPorId(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Sala cirúrgica não encontrada (id 99).");
	}

}
