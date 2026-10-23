package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ConsultaTest {

	private final Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
	private final ProfissionalSaude medico = new ClinicoGeral("Dr. João Souza", "CRM-MG 12345");

	private Consulta novaConsulta() {
		return new ConsultaRotina(paciente, medico, LocalDate.of(2030, 1, 15), LocalTime.of(14, 0), "Dor no peito");
	}

	// Leva uma consulta nova até a situação pedida pelo caminho válido mais curto.
	private Consulta consultaEm(SituacaoConsulta situacao) {
		Consulta consulta = novaConsulta();
		switch (situacao) {
			case AGENDADA -> {
			}
			case CONFIRMADA -> consulta.confirmar();
			case EM_ATENDIMENTO -> {
				consulta.confirmar();
				consulta.iniciarAtendimento();
			}
			case CONCLUIDA -> {
				consulta.confirmar();
				consulta.iniciarAtendimento();
				consulta.concluir();
			}
			case CANCELADA -> consulta.cancelar();
			case NAO_COMPARECEU -> consulta.registrarNaoComparecimento();
		}
		return consulta;
	}

	private enum Acao {
		CONFIRMAR(SituacaoConsulta.CONFIRMADA, Consulta::confirmar),
		INICIAR_ATENDIMENTO(SituacaoConsulta.EM_ATENDIMENTO, Consulta::iniciarAtendimento),
		CONCLUIR(SituacaoConsulta.CONCLUIDA, Consulta::concluir),
		CANCELAR(SituacaoConsulta.CANCELADA, Consulta::cancelar),
		REGISTRAR_NAO_COMPARECIMENTO(SituacaoConsulta.NAO_COMPARECEU, Consulta::registrarNaoComparecimento);

		private final SituacaoConsulta destino;
		private final Consumer<Consulta> executar;

		Acao(SituacaoConsulta destino, Consumer<Consulta> executar) {
			this.destino = destino;
			this.executar = executar;
		}
	}

	static Stream<Arguments> cadaAcaoEmCadaSituacao() {
		return Arrays.stream(Acao.values())
				.flatMap(acao -> Arrays.stream(SituacaoConsulta.values()).map(situacao -> Arguments.of(acao, situacao)));
	}

	@Test
	void novaConsultaComecaAgendada() {
		assertThat(novaConsulta().getSituacao()).isEqualTo(SituacaoConsulta.AGENDADA);
	}

	@Test
	void fluxoCompletoAteAConclusao() {
		Consulta consulta = novaConsulta();

		consulta.confirmar();
		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.CONFIRMADA);
		consulta.iniciarAtendimento();
		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.EM_ATENDIMENTO);
		consulta.concluir();
		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.CONCLUIDA);
	}

	@ParameterizedTest(name = "{0} a partir de {1}")
	@MethodSource("cadaAcaoEmCadaSituacao")
	void acaoSoMudaASituacaoQuandoATransicaoEPermitida(Acao acao, SituacaoConsulta situacaoInicial) {
		Consulta consulta = consultaEm(situacaoInicial);

		if (situacaoInicial.podeMudarPara(acao.destino)) {
			acao.executar.accept(consulta);
			assertThat(consulta.getSituacao()).isEqualTo(acao.destino);
		} else {
			assertThatThrownBy(() -> acao.executar.accept(consulta))
					.isInstanceOf(TransicaoSituacaoInvalidaException.class);
			assertThat(consulta.getSituacao()).isEqualTo(situacaoInicial);
		}
	}

	@Test
	void mensagemDeTransicaoInvalidaInformaASituacaoAtual() {
		Consulta consulta = novaConsulta();

		assertThatThrownBy(consulta::concluir)
				.isInstanceOf(TransicaoSituacaoInvalidaException.class)
				.hasMessage("Não é possível concluir uma consulta na situação Agendada.");
	}

	@Test
	void mensagemDeTransicaoInvalidaAvisaQuandoASituacaoEFinal() {
		Consulta consulta = consultaEm(SituacaoConsulta.CANCELADA);

		assertThatThrownBy(consulta::confirmar)
				.isInstanceOf(TransicaoSituacaoInvalidaException.class)
				.hasMessage("Não é possível confirmar uma consulta na situação Cancelada. "
						+ "Essa situação é final e não pode mais ser alterada.");
	}

	@Test
	void naoIniciaAtendimentoDePacienteInativo() {
		Consulta consulta = consultaEm(SituacaoConsulta.CONFIRMADA);
		paciente.inativar();

		assertThatThrownBy(consulta::iniciarAtendimento)
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O paciente Maria Silva está inativo e não pode ser atendido.");
		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.CONFIRMADA);
	}

	@Test
	void naoIniciaAtendimentoComProfissionalInativo() {
		Consulta consulta = consultaEm(SituacaoConsulta.CONFIRMADA);
		medico.inativar();

		assertThatThrownBy(consulta::iniciarAtendimento)
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O profissional Dr. João Souza está inativo e não pode realizar atendimentos.");
		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.CONFIRMADA);
	}

	@Test
	void transicaoInvalidaEVerificadaAntesDaRealizacao() {
		Consulta consulta = novaConsulta();
		paciente.inativar();

		assertThatThrownBy(consulta::iniciarAtendimento).isInstanceOf(TransicaoSituacaoInvalidaException.class);
	}

	@Test
	void cancelarNaoExigePacienteAtivo() {
		Consulta consulta = novaConsulta();
		paciente.inativar();

		consulta.cancelar();

		assertThat(consulta.getSituacao()).isEqualTo(SituacaoConsulta.CANCELADA);
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoConsulta.class, names = { "EM_ATENDIMENTO", "CONCLUIDA" })
	void registraObservacoesDuranteOuDepoisDoAtendimento(SituacaoConsulta situacao) {
		Consulta consulta = consultaEm(situacao);

		consulta.registrarObservacoes("Pressão arterial 12x8.");

		assertThat(consulta.getObservacoes()).isEqualTo("Pressão arterial 12x8.");
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoConsulta.class, names = { "AGENDADA", "CONFIRMADA", "CANCELADA", "NAO_COMPARECEU" })
	void naoRegistraObservacoesForaDoAtendimento(SituacaoConsulta situacao) {
		Consulta consulta = consultaEm(situacao);

		assertThatThrownBy(() -> consulta.registrarObservacoes("Pressão arterial 12x8."))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageStartingWith("As observações só podem ser registradas durante o atendimento");
		assertThat(consulta.getObservacoes()).isNull();
	}

}
