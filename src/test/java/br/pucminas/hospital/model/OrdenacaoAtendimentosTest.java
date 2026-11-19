package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrdenacaoAtendimentosTest {

	private static final LocalDate DIA = LocalDate.of(2030, 3, 10);
	private static final LocalDateTime NOITE = DIA.atTime(22, 0);

	private final ProfissionalSaude clinico = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");

	private static Paciente paciente(String nome) {
		return new Paciente(nome, "52998224725", LocalDate.of(1990, 5, 10));
	}

	private Consulta consulta(Paciente paciente, String horario) {
		return new ConsultaRotina(paciente, clinico, DIA, LocalTime.parse(horario), "Retorno");
	}

	private AtendimentoEmergencia emergencia(PrioridadeEmergencia prioridade, int minutosDepois) {
		return new AtendimentoEmergencia(paciente("Maria Silva"), clinico, prioridade,
				NOITE.plusMinutes(minutosDepois));
	}

	private static List<Atendimento> ordenar(List<? extends Atendimento> atendimentos, OrdenacaoAtendimentos ordem) {
		return atendimentos.stream().map(Atendimento.class::cast).sorted(ordem.comparador()).toList();
	}

	@Test
	void porDataDaMaisAntigaParaAMaisRecente() {
		Paciente maria = paciente("Maria Silva");
		Consulta tarde = consulta(maria, "15:00");
		Consulta manha = consulta(maria, "09:00");
		AtendimentoEmergencia noite = emergencia(null, 0);

		assertThat(ordenar(List.of(noite, tarde, manha), OrdenacaoAtendimentos.DATA)).containsExactly(manha, tarde,
				noite);
		assertThat(ordenar(List.of(manha, noite, tarde), OrdenacaoAtendimentos.DATA_DECRESCENTE))
				.containsExactly(noite, tarde, manha);
	}

	@Test
	void mesmoMomentoDesempataPeloId() {
		Paciente maria = paciente("Maria Silva");
		Consulta segunda = consulta(maria, "09:00");
		Consulta primeira = consulta(maria, "09:00");
		ReflectionTestUtils.setField(segunda, "id", 2L);
		ReflectionTestUtils.setField(primeira, "id", 1L);

		assertThat(ordenar(List.of(segunda, primeira), OrdenacaoAtendimentos.DATA)).containsExactly(primeira, segunda);
	}

	@Test
	void porNomeDoPacienteSemSerAfetadoPorAcentosEMaiusculasEDepoisPorData() {
		Consulta bruno = consulta(paciente("Bruno Costa"), "08:00");
		Consulta alvaro = consulta(paciente("Álvaro Dias"), "09:00");
		Paciente ana = paciente("ana Souza");
		Consulta anaTarde = consulta(ana, "16:00");
		Consulta anaManha = consulta(ana, "10:00");

		assertThat(ordenar(List.of(bruno, anaTarde, alvaro, anaManha), OrdenacaoAtendimentos.PACIENTE))
				.containsExactly(alvaro, anaManha, anaTarde, bruno);
	}

	// A fila de emergência: prioridade mais grave primeiro, depois a chegada, e sem prioridade no fim.
	@Test
	void porPrioridadeDaMaisGraveParaAMenosGraveEDepoisPelaChegada() {
		AtendimentoEmergencia baixa = emergencia(PrioridadeEmergencia.BAIXA, 30);
		AtendimentoEmergencia criticaDepois = emergencia(PrioridadeEmergencia.CRITICA, 50);
		AtendimentoEmergencia semPrioridadeAntes = emergencia(null, 0);
		AtendimentoEmergencia alta = emergencia(PrioridadeEmergencia.ALTA, 40);
		AtendimentoEmergencia criticaAntes = emergencia(PrioridadeEmergencia.CRITICA, 45);
		AtendimentoEmergencia moderada = emergencia(PrioridadeEmergencia.MODERADA, 10);
		AtendimentoEmergencia semPrioridadeDepois = emergencia(null, 5);

		assertThat(ordenar(List.of(baixa, criticaDepois, semPrioridadeAntes, alta, criticaAntes, moderada,
				semPrioridadeDepois), OrdenacaoAtendimentos.PRIORIDADE))
				.containsExactly(criticaAntes, criticaDepois, alta, moderada, baixa, semPrioridadeAntes,
						semPrioridadeDepois);
	}

	// Garante que a fila segue o nível de cada prioridade, e não a ordem em que as constantes foram declaradas.
	@Test
	void prioridadeSegueONivelDoEnum() {
		List<Atendimento> fila = ordenar(List.of(PrioridadeEmergencia.values()).stream()
				.map(prioridade -> emergencia(prioridade, 0)).toList(), OrdenacaoAtendimentos.PRIORIDADE);

		assertThat(fila).extracting(atendimento -> atendimento.getPrioridade().getNivel()).isSortedAccordingTo(
				(a, b) -> Integer.compare(b, a));
	}

	@Test
	void porPrioridadeAsOutrasModalidadesFicamDepoisDasEmergencias() {
		Consulta consultaAntes = consulta(paciente("Maria Silva"), "08:00");
		AtendimentoEmergencia emergencia = emergencia(PrioridadeEmergencia.BAIXA, 0);

		assertThat(ordenar(List.of(consultaAntes, emergencia), OrdenacaoAtendimentos.PRIORIDADE))
				.containsExactly(emergencia, consultaAntes);
	}

}
