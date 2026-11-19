package br.pucminas.hospital.model;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Critérios de ordenação de atendimentos. Cada constante guarda o seu {@link Comparator}, então escolher a
 * ordenação é escolher a constante; os critérios que podem empatar desempatam pela data.
 */
public enum OrdenacaoAtendimentos implements Descritivel {

	DATA("Data, da mais antiga para a mais recente", Comparadores.POR_DATA),
	DATA_DECRESCENTE("Data, da mais recente para a mais antiga", Comparadores.POR_DATA.reversed()),
	PACIENTE("Nome do paciente, em ordem alfabética", Comparadores.POR_PACIENTE.thenComparing(Comparadores.POR_DATA)),
	PRIORIDADE("Prioridade de emergência, da mais grave para a menos grave",
			Comparadores.POR_PRIORIDADE.thenComparing(Comparadores.POR_DATA));

	private final String descricao;
	private final Comparator<Atendimento> comparador;

	OrdenacaoAtendimentos(String descricao, Comparator<Atendimento> comparador) {
		this.descricao = descricao;
		this.comparador = comparador;
	}

	public Comparator<Atendimento> comparador() {
		return comparador;
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

	// Numa classe à parte porque as constantes do enum são criadas antes dos campos estáticos dele.
	private static final class Comparadores {

		// A data que situa o atendimento; o id desempata atendimentos no mesmo momento.
		static final Comparator<Atendimento> POR_DATA = Comparator.comparing(Atendimento::getDataHoraReferencia)
				.thenComparing(Atendimento::getId, Comparator.nullsLast(Comparator.naturalOrder()));

		// Collator para que acentos e maiúsculas não alterem a ordem alfabética (Álvaro antes de Bruno).
		static final Comparator<Atendimento> POR_PACIENTE = Comparator.comparing(
				atendimento -> atendimento.getPaciente().getNome(), Collator.getInstance(Locale.of("pt", "BR")));

		// Nível maior é mais grave. Sem prioridade (emergência ainda não avaliada ou outra modalidade), no fim.
		static final Comparator<Atendimento> POR_PRIORIDADE = Comparator.comparing(Atendimento::getPrioridade,
				Comparator.nullsLast(Comparator.comparingInt(PrioridadeEmergencia::getNivel).reversed()));

	}

}
