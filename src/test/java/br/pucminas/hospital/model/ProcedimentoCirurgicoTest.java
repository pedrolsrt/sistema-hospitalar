package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProcedimentoCirurgicoTest {

	private static final LocalDateTime PREVISTA = LocalDateTime.of(2030, 3, 10, 8, 0);
	private static final LocalDateTime INICIO = PREVISTA.plusMinutes(15);

	private final Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
	private final Cirurgiao cirurgiao = new Cirurgiao("Dra. Carla Mendes", "CRM-MG 33333", AreaAtuacao.CIRURGIA_GERAL);

	private ProcedimentoCirurgico agendar() {
		return new ProcedimentoCirurgico(paciente, cirurgiao, "Apendicectomia", TipoCirurgia.URGENCIA, PREVISTA);
	}

	private ProcedimentoCirurgico procedimentoEm(SituacaoProcedimento situacao) {
		ProcedimentoCirurgico procedimento = agendar();
		switch (situacao) {
			case AGENDADO -> {
			}
			case EM_ANDAMENTO -> procedimento.iniciar(INICIO);
			case CONCLUIDO -> {
				procedimento.iniciar(INICIO);
				procedimento.concluir(INICIO.plusHours(1));
			}
			case CANCELADO -> procedimento.cancelar();
		}
		return procedimento;
	}

	static Stream<ProfissionalSaude> profissionaisQueNaoSaoCirurgioes() {
		return Stream.of(
				new ClinicoGeral("Dra. Ana Lima", "CRM-MG 11111"),
				new MedicoEspecialista("Dr. João Souza", "CRM-MG 22222", AreaAtuacao.CIRURGIA_GERAL));
	}

	@Test
	void agendamentoComCirurgiao() {
		ProcedimentoCirurgico procedimento = agendar();

		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.AGENDADO);
		assertThat(procedimento.getDataHoraInicio()).isNull();
		assertThat(procedimento.getDataHoraFim()).isNull();
	}

	@ParameterizedTest
	@MethodSource("profissionaisQueNaoSaoCirurgioes")
	void naoAgendaComProfissionalQueNaoECirurgiao(ProfissionalSaude profissional) {
		assertThatThrownBy(() -> new ProcedimentoCirurgico(paciente, profissional, "Apendicectomia",
				TipoCirurgia.URGENCIA, PREVISTA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O profissional " + profissional.getNome()
						+ " não pode realizar procedimentos cirúrgicos: é necessário um cirurgião.");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void exigeDescricao(String descricao) {
		assertThatThrownBy(() -> new ProcedimentoCirurgico(paciente, cirurgiao, descricao, TipoCirurgia.ELETIVA, PREVISTA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("A descrição do procedimento cirúrgico é obrigatória.");
	}

	@Test
	void exigeTipoDeCirurgia() {
		assertThatThrownBy(() -> new ProcedimentoCirurgico(paciente, cirurgiao, "Apendicectomia", null, PREVISTA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O tipo de cirurgia é obrigatório.");
	}

	@Test
	void exigeDataHoraPrevista() {
		assertThatThrownBy(() -> new ProcedimentoCirurgico(paciente, cirurgiao, "Apendicectomia",
				TipoCirurgia.ELETIVA, null))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void fluxoCompleto() {
		ProcedimentoCirurgico procedimento = agendar();

		procedimento.iniciar(INICIO);
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.EM_ANDAMENTO);
		assertThat(procedimento.getDataHoraInicio()).isEqualTo(INICIO);
		procedimento.concluir(INICIO.plusHours(2));
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.CONCLUIDO);
		assertThat(procedimento.getDataHoraFim()).isEqualTo(INICIO.plusHours(2));
	}

	@Test
	void naoConcluiSemTerIniciado() {
		ProcedimentoCirurgico procedimento = agendar();

		assertThatThrownBy(() -> procedimento.concluir(INICIO.plusHours(1)))
				.isInstanceOf(TransicaoSituacaoInvalidaException.class)
				.hasMessage("Não é possível concluir um procedimento cirúrgico na situação Agendado.");
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.AGENDADO);
		assertThat(procedimento.getDataHoraFim()).isNull();
	}

	@Test
	void naoConcluiComFimAnteriorAoInicio() {
		ProcedimentoCirurgico procedimento = procedimentoEm(SituacaoProcedimento.EM_ANDAMENTO);

		assertThatThrownBy(() -> procedimento.concluir(INICIO.minusMinutes(1)))
				.isInstanceOf(DataInvalidaException.class)
				.hasMessage("A data e hora de fim do procedimento (10/03/2030 08:14) não pode ser anterior ao início "
						+ "(10/03/2030 08:15).");
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.EM_ANDAMENTO);
		assertThat(procedimento.getDataHoraFim()).isNull();
	}

	@Test
	void concluiComFimIgualAoInicio() {
		ProcedimentoCirurgico procedimento = procedimentoEm(SituacaoProcedimento.EM_ANDAMENTO);

		procedimento.concluir(INICIO);

		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.CONCLUIDO);
	}

	@Test
	void exigeDatasAoIniciarEConcluir() {
		ProcedimentoCirurgico procedimento = agendar();
		assertThatThrownBy(() -> procedimento.iniciar(null)).isInstanceOf(RegraNegocioException.class);
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.AGENDADO);

		procedimento.iniciar(INICIO);
		assertThatThrownBy(() -> procedimento.concluir(null)).isInstanceOf(RegraNegocioException.class);
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.EM_ANDAMENTO);
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoProcedimento.class, names = { "EM_ANDAMENTO", "CONCLUIDO", "CANCELADO" })
	void soIniciaProcedimentoAgendado(SituacaoProcedimento situacao) {
		ProcedimentoCirurgico procedimento = procedimentoEm(situacao);

		assertThatThrownBy(() -> procedimento.iniciar(INICIO)).isInstanceOf(TransicaoSituacaoInvalidaException.class);
		assertThat(procedimento.getSituacao()).isEqualTo(situacao);
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoProcedimento.class, names = { "AGENDADO", "CONCLUIDO", "CANCELADO" })
	void soConcluiProcedimentoEmAndamento(SituacaoProcedimento situacao) {
		ProcedimentoCirurgico procedimento = procedimentoEm(situacao);

		assertThatThrownBy(() -> procedimento.concluir(INICIO.plusHours(3)))
				.isInstanceOf(TransicaoSituacaoInvalidaException.class);
		assertThat(procedimento.getSituacao()).isEqualTo(situacao);
	}

	@Test
	void cancelaProcedimentoAgendado() {
		ProcedimentoCirurgico procedimento = agendar();

		procedimento.cancelar();

		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.CANCELADO);
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoProcedimento.class, names = { "EM_ANDAMENTO", "CONCLUIDO", "CANCELADO" })
	void soCancelaProcedimentoAgendado(SituacaoProcedimento situacao) {
		ProcedimentoCirurgico procedimento = procedimentoEm(situacao);

		assertThatThrownBy(procedimento::cancelar).isInstanceOf(TransicaoSituacaoInvalidaException.class);
		assertThat(procedimento.getSituacao()).isEqualTo(situacao);
	}

	@Test
	void naoIniciaSeOCirurgiaoFoiInativadoDepoisDoAgendamento() {
		ProcedimentoCirurgico procedimento = agendar();
		cirurgiao.inativar();

		assertThatThrownBy(() -> procedimento.iniciar(INICIO))
				.hasMessage("O profissional Dra. Carla Mendes está inativo e não pode realizar atendimentos.");
		assertThat(procedimento.getSituacao()).isEqualTo(SituacaoProcedimento.AGENDADO);
	}

	@ParameterizedTest
	@EnumSource(SituacaoProcedimento.class)
	void registraObservacoesPreOperatoriasEmQualquerSituacao(SituacaoProcedimento situacao) {
		ProcedimentoCirurgico procedimento = procedimentoEm(situacao);

		procedimento.registrarObservacoesPreOperatorias("Jejum de 8 horas.");

		assertThat(procedimento.getObservacoesPreOperatorias()).isEqualTo("Jejum de 8 horas.");
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoProcedimento.class, names = { "EM_ANDAMENTO", "CONCLUIDO" })
	void registraObservacoesPosOperatoriasDepoisDoInicio(SituacaoProcedimento situacao) {
		ProcedimentoCirurgico procedimento = procedimentoEm(situacao);

		procedimento.registrarObservacoesPosOperatorias("Sem intercorrências.");

		assertThat(procedimento.getObservacoesPosOperatorias()).isEqualTo("Sem intercorrências.");
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoProcedimento.class, names = { "AGENDADO", "CANCELADO" })
	void naoRegistraObservacoesPosOperatoriasAntesDoInicio(SituacaoProcedimento situacao) {
		ProcedimentoCirurgico procedimento = procedimentoEm(situacao);

		assertThatThrownBy(() -> procedimento.registrarObservacoesPosOperatorias("Sem intercorrências."))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageStartingWith("As observações pós-operatórias só podem ser registradas depois que o "
						+ "procedimento é iniciado.");
		assertThat(procedimento.getObservacoesPosOperatorias()).isNull();
	}

}
