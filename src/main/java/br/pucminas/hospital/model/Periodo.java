package br.pucminas.hospital.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Intervalo de tempo ocupado por um atendimento, do início (incluído) ao fim (excluído). Por ser
 * semiaberto, um período que termina exatamente quando outro começa não se sobrepõe a ele.
 */
public record Periodo(LocalDateTime inicio, LocalDateTime fim) {

	private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

	/**
	 * @throws IllegalArgumentException se o início ou o fim faltar, ou se o fim não for posterior ao início
	 */
	public Periodo {
		if (inicio == null || fim == null || !fim.isAfter(inicio)) {
			throw new IllegalArgumentException("Período inválido: " + inicio + " a " + fim + ".");
		}
	}

	public static Periodo de(LocalDateTime inicio, Duration duracao) {
		return new Periodo(inicio, inicio.plus(duracao));
	}

	/**
	 * Dois períodos se sobrepõem se cada um começa antes do fim do outro.
	 */
	public boolean sobrepoe(Periodo outro) {
		return inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
	}

	/**
	 * Texto para mensagens, por exemplo "15/01/2030, das 14:00 às 14:30" ou, se o período passar da
	 * meia-noite, "15/01/2030 às 23:50 até 16/01/2030 às 00:20".
	 */
	public String descricao() {
		if (inicio.toLocalDate().equals(fim.toLocalDate())) {
			return FORMATO_DATA.format(inicio) + ", das " + FORMATO_HORA.format(inicio) + " às "
					+ FORMATO_HORA.format(fim);
		}
		return FORMATO_DATA.format(inicio) + " às " + FORMATO_HORA.format(inicio) + " até " + FORMATO_DATA.format(fim)
				+ " às " + FORMATO_HORA.format(fim);
	}

}
