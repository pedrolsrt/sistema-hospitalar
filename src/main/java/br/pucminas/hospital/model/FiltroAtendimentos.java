package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.DataInvalidaException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Critérios opcionais de busca de atendimentos; um critério nulo não filtra nada. Cada critério é um
 * {@link Predicate} com nome próprio, e {@link #comoPredicado()} combina com {@code and()} só os informados,
 * o que dispensa um método de busca para cada combinação.
 *
 * @param situacao código da situação na modalidade do atendimento, por exemplo "CONCLUIDA" ou "ATIVA"
 * @param inicio primeira data do período, comparada com a data de referência do atendimento
 * @param fim última data do período, incluída
 */
public record FiltroAtendimentos(Long pacienteId, Long profissionalId, String situacao,
		ModalidadeAtendimento modalidade, LocalDate inicio, LocalDate fim) {

	private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	/**
	 * Códigos de situação de todas as modalidades, sem repetição (CONCLUIDO é da emergência e do procedimento).
	 */
	public static Set<String> situacoesReconhecidas() {
		Set<String> codigos = new LinkedHashSet<>();
		Stream.of(SituacaoConsulta.values(), SituacaoEmergencia.values(), SituacaoProcedimento.values(),
				SituacaoInternacao.values())
				.flatMap(Stream::of)
				.forEach(situacao -> codigos.add(situacao.name()));
		return codigos;
	}

	/**
	 * @throws DataInvalidaException se o início do período for posterior ao fim
	 */
	public FiltroAtendimentos {
		if (inicio != null && fim != null && inicio.isAfter(fim)) {
			throw new DataInvalidaException("O início do período (" + FORMATO_DATA.format(inicio)
					+ ") não pode ser posterior ao fim (" + FORMATO_DATA.format(fim) + ").");
		}
	}

	/**
	 * Junta os critérios informados; sem nenhum, aceita todos os atendimentos.
	 */
	public Predicate<Atendimento> comoPredicado() {
		Predicate<Atendimento> filtro = atendimento -> true;
		if (pacienteId != null) {
			filtro = filtro.and(doPaciente(pacienteId));
		}
		if (profissionalId != null) {
			filtro = filtro.and(doProfissional(profissionalId));
		}
		if (situacao != null) {
			filtro = filtro.and(naSituacao(situacao));
		}
		if (modalidade != null) {
			filtro = filtro.and(daModalidade(modalidade));
		}
		if (inicio != null) {
			filtro = filtro.and(aPartirDe(inicio));
		}
		if (fim != null) {
			filtro = filtro.and(ate(fim));
		}
		return filtro;
	}

	public static Predicate<Atendimento> doPaciente(Long pacienteId) {
		return atendimento -> pacienteId.equals(atendimento.getPaciente().getId());
	}

	public static Predicate<Atendimento> doProfissional(Long profissionalId) {
		return atendimento -> profissionalId.equals(atendimento.getProfissionalResponsavel().getId());
	}

	/**
	 * Compara o código da situação, o que permite filtrar modalidades com enums de situação diferentes.
	 */
	public static Predicate<Atendimento> naSituacao(String codigoSituacao) {
		return atendimento -> atendimento.getSituacao().name().equals(codigoSituacao);
	}

	public static Predicate<Atendimento> daModalidade(ModalidadeAtendimento modalidade) {
		return atendimento -> atendimento.getModalidade() == modalidade;
	}

	/**
	 * Atendimentos cuja data de referência é o dia informado ou um dia posterior.
	 */
	public static Predicate<Atendimento> aPartirDe(LocalDate inicio) {
		return atendimento -> !atendimento.getDataHoraReferencia().toLocalDate().isBefore(inicio);
	}

	/**
	 * Atendimentos cuja data de referência é o dia informado ou um dia anterior.
	 */
	public static Predicate<Atendimento> ate(LocalDate fim) {
		return atendimento -> !atendimento.getDataHoraReferencia().toLocalDate().isAfter(fim);
	}

}
