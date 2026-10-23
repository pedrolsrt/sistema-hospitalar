package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.RegraNegocioException;
import br.pucminas.hospital.exception.TransicaoSituacaoInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AtendimentoEmergenciaTest {

	private static final LocalDateTime CHEGADA = LocalDateTime.of(2030, 1, 15, 22, 40);

	private final Paciente paciente = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
	private final ProfissionalSaude plantonista = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 11111");

	private AtendimentoEmergencia registrar(PrioridadeEmergencia prioridade) {
		return new AtendimentoEmergencia(paciente, plantonista, prioridade, CHEGADA);
	}

	private AtendimentoEmergencia emergenciaEm(SituacaoEmergencia situacao) {
		AtendimentoEmergencia emergencia = registrar(PrioridadeEmergencia.ALTA);
		if (situacao != SituacaoEmergencia.AGUARDANDO_AVALIACAO) {
			emergencia.iniciar();
		}
		if (situacao == SituacaoEmergencia.CONCLUIDO) {
			emergencia.concluir();
		}
		return emergencia;
	}

	@Test
	void registroComecaAguardandoAvaliacaoComOPlantonista() {
		AtendimentoEmergencia emergencia = registrar(PrioridadeEmergencia.CRITICA);

		assertThat(emergencia.getSituacao()).isEqualTo(SituacaoEmergencia.AGUARDANDO_AVALIACAO);
		assertThat(emergencia.getProfissionalResponsavel()).isSameAs(plantonista);
		assertThat(emergencia.getDataHoraChegada()).isEqualTo(CHEGADA);
	}

	@Test
	void fluxoCompleto() {
		AtendimentoEmergencia emergencia = registrar(PrioridadeEmergencia.MODERADA);

		emergencia.iniciar();
		assertThat(emergencia.getSituacao()).isEqualTo(SituacaoEmergencia.EM_ATENDIMENTO);
		emergencia.concluir();
		assertThat(emergencia.getSituacao()).isEqualTo(SituacaoEmergencia.CONCLUIDO);
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoEmergencia.class, names = { "EM_ATENDIMENTO", "CONCLUIDO" })
	void naoIniciaForaDeAguardandoAvaliacao(SituacaoEmergencia situacao) {
		AtendimentoEmergencia emergencia = emergenciaEm(situacao);

		assertThatThrownBy(emergencia::iniciar).isInstanceOf(TransicaoSituacaoInvalidaException.class);
		assertThat(emergencia.getSituacao()).isEqualTo(situacao);
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoEmergencia.class, names = { "AGUARDANDO_AVALIACAO", "CONCLUIDO" })
	void naoConcluiForaDeEmAtendimento(SituacaoEmergencia situacao) {
		AtendimentoEmergencia emergencia = emergenciaEm(situacao);

		assertThatThrownBy(emergencia::concluir).isInstanceOf(TransicaoSituacaoInvalidaException.class);
		assertThat(emergencia.getSituacao()).isEqualTo(situacao);
	}

	@Test
	void mensagemDeConcluirSemIniciar() {
		AtendimentoEmergencia emergencia = registrar(PrioridadeEmergencia.ALTA);

		assertThatThrownBy(emergencia::concluir)
				.hasMessage("Não é possível concluir um atendimento de emergência na situação Aguardando avaliação.");
	}

	@Test
	void naoIniciaSemPrioridadeDefinida() {
		AtendimentoEmergencia emergencia = registrar(null);

		assertThatThrownBy(emergencia::iniciar)
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Defina a prioridade do atendimento de emergência antes de iniciar o atendimento.");
		assertThat(emergencia.getSituacao()).isEqualTo(SituacaoEmergencia.AGUARDANDO_AVALIACAO);
	}

	@Test
	void iniciaDepoisDeDefinirAPrioridade() {
		AtendimentoEmergencia emergencia = registrar(null);
		emergencia.definirPrioridade(PrioridadeEmergencia.CRITICA);

		emergencia.iniciar();

		assertThat(emergencia.getSituacao()).isEqualTo(SituacaoEmergencia.EM_ATENDIMENTO);
		assertThat(emergencia.getPrioridade()).isEqualTo(PrioridadeEmergencia.CRITICA);
	}

	@Test
	void reclassificaPrioridadeDuranteOAtendimento() {
		AtendimentoEmergencia emergencia = emergenciaEm(SituacaoEmergencia.EM_ATENDIMENTO);

		emergencia.definirPrioridade(PrioridadeEmergencia.CRITICA);

		assertThat(emergencia.getPrioridade()).isEqualTo(PrioridadeEmergencia.CRITICA);
	}

	@Test
	void naoAlteraPrioridadeDeAtendimentoConcluido() {
		AtendimentoEmergencia emergencia = emergenciaEm(SituacaoEmergencia.CONCLUIDO);

		assertThatThrownBy(() -> emergencia.definirPrioridade(PrioridadeEmergencia.BAIXA))
				.isInstanceOf(RegraNegocioException.class);
		assertThat(emergencia.getPrioridade()).isEqualTo(PrioridadeEmergencia.ALTA);
	}

	@Test
	void naoAceitaPrioridadeNula() {
		AtendimentoEmergencia emergencia = registrar(PrioridadeEmergencia.ALTA);

		assertThatThrownBy(() -> emergencia.definirPrioridade(null)).isInstanceOf(RegraNegocioException.class);
		assertThat(emergencia.getPrioridade()).isEqualTo(PrioridadeEmergencia.ALTA);
	}

	@Test
	void naoIniciaComPacienteInativo() {
		AtendimentoEmergencia emergencia = registrar(PrioridadeEmergencia.ALTA);
		paciente.inativar();

		assertThatThrownBy(emergencia::iniciar).hasMessage("O paciente Maria Silva está inativo e não pode ser atendido.");
		assertThat(emergencia.getSituacao()).isEqualTo(SituacaoEmergencia.AGUARDANDO_AVALIACAO);
	}

	@Test
	void exigeDataHoraDeChegada() {
		assertThatThrownBy(() -> new AtendimentoEmergencia(paciente, plantonista, PrioridadeEmergencia.ALTA, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("A data e hora de chegada do paciente são obrigatórias.");
	}

}
