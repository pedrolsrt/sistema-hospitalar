package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.AtendimentosPorModalidadeResponse;
import br.pucminas.hospital.dto.AtendimentosPorProfissionalResponse;
import br.pucminas.hospital.dto.CodigoDescricao;
import br.pucminas.hospital.dto.IndicadoresResponse;
import br.pucminas.hospital.dto.QuartosPorSituacaoResponse;
import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.model.Atendimento;
import br.pucminas.hospital.model.FiltroAtendimentos;
import br.pucminas.hospital.model.ModalidadeAtendimento;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.SituacaoAtendimento;
import br.pucminas.hospital.model.SituacaoConsulta;
import br.pucminas.hospital.model.SituacaoQuarto;
import br.pucminas.hospital.repository.AtendimentoRepository;
import br.pucminas.hospital.repository.QuartoRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores gerenciais, calculados em memória por um único ponto de contagem, {@link #contar}: o
 * {@link Predicate} escolhe o que entra e a {@link Function} define o critério de agrupamento. Cada indicador é
 * só uma combinação diferente dos dois, sem um método de contagem próprio.
 */
@Service
@Transactional(readOnly = true)
public class IndicadorService {

	/**
	 * Separa os atendimentos pela situação, para contar as consultas realizadas: CONCLUIDA só existe na consulta,
	 * então o grupo {@link SituacaoConsulta#CONCLUIDA} tem exatamente as consultas realizadas.
	 */
	static final Function<Atendimento, SituacaoAtendimento<?>> POR_SITUACAO = Atendimento::getSituacao;

	/** Separa os atendimentos pela modalidade, para o indicador de atendimentos por modalidade. */
	static final Function<Atendimento, ModalidadeAtendimento> POR_MODALIDADE = Atendimento::getModalidade;

	/** Separa os atendimentos pelo profissional responsável, para o indicador de atendimentos por profissional. */
	static final Function<Atendimento, ProfissionalSaude> POR_PROFISSIONAL = Atendimento::getProfissionalResponsavel;

	/** Separa os quartos em disponíveis e ocupados, pela situação atual, que depende da ocupação. */
	static final Function<Quarto, SituacaoQuarto> POR_SITUACAO_DO_QUARTO = Quarto::getSituacao;

	/** Deixa passar todos os quartos: o indicador de quartos mostra a situação de agora e não usa o período. */
	static final Predicate<Quarto> TODOS_OS_QUARTOS = quarto -> true;

	/** Ordem dos profissionais no indicador: quem tem mais atendimentos primeiro; empatados, pelo nome. */
	private static final Comparator<Map.Entry<ProfissionalSaude, Long>> MAIS_ATENDIMENTOS_PRIMEIRO =
			Map.Entry.<ProfissionalSaude, Long>comparingByValue().reversed()
					.thenComparing(entrada -> entrada.getKey().getNome());

	private final AtendimentoRepository atendimentoRepository;
	private final QuartoRepository quartoRepository;

	public IndicadorService(AtendimentoRepository atendimentoRepository, QuartoRepository quartoRepository) {
		this.atendimentoRepository = atendimentoRepository;
		this.quartoRepository = quartoRepository;
	}

	/**
	 * Calcula os indicadores. O período é opcional e inclui as duas datas; vale para os atendimentos, comparando
	 * a data de referência de cada um, como na busca de atendimentos.
	 *
	 * @throws DataInvalidaException se o início do período for posterior ao fim
	 */
	public IndicadoresResponse calcular(LocalDate inicio, LocalDate fim) {
		Predicate<Atendimento> noPeriodo = noPeriodo(inicio, fim);
		List<Atendimento> atendimentos = atendimentoRepository.findAll();

		Map<SituacaoAtendimento<?>, Long> porSituacao = contar(atendimentos, noPeriodo, POR_SITUACAO);
		Map<ModalidadeAtendimento, Long> porModalidade = contar(atendimentos, noPeriodo, POR_MODALIDADE);
		Map<ProfissionalSaude, Long> porProfissional = contar(atendimentos, noPeriodo, POR_PROFISSIONAL);
		Map<SituacaoQuarto, Long> quartos = contar(quartoRepository.findAll(), TODOS_OS_QUARTOS, POR_SITUACAO_DO_QUARTO);

		return new IndicadoresResponse(inicio, fim,
				porSituacao.getOrDefault(SituacaoConsulta.CONCLUIDA, 0L),
				Stream.of(ModalidadeAtendimento.values())
						.map(modalidade -> new AtendimentosPorModalidadeResponse(CodigoDescricao.de(modalidade),
								porModalidade.getOrDefault(modalidade, 0L)))
						.toList(),
				new QuartosPorSituacaoResponse(quartos.getOrDefault(SituacaoQuarto.DISPONIVEL, 0L),
						quartos.getOrDefault(SituacaoQuarto.OCUPADO, 0L)),
				porProfissional.entrySet().stream()
						.sorted(MAIS_ATENDIMENTOS_PRIMEIRO)
						.map(entrada -> new AtendimentosPorProfissionalResponse(entrada.getKey().getId(),
								entrada.getKey().getNome(), CodigoDescricao.de(entrada.getKey().getCategoria()),
								entrada.getValue()))
						.toList());
	}

	/**
	 * Ponto único de contagem: filtra os itens e conta quantos há em cada grupo do critério. Grupos sem itens não
	 * aparecem no mapa.
	 */
	static <T, K> Map<K, Long> contar(Collection<T> itens, Predicate<? super T> filtro,
			Function<? super T, ? extends K> criterio) {
		return itens.stream()
				.filter(filtro)
				.collect(Collectors.groupingBy(criterio, Collectors.counting()));
	}

	/**
	 * Deixa passar os atendimentos do período, com o mesmo critério da busca de atendimentos: a data de referência
	 * entre o início e o fim, inclusive. Sem datas, passam todos.
	 *
	 * @throws DataInvalidaException se o início do período for posterior ao fim
	 */
	private static Predicate<Atendimento> noPeriodo(LocalDate inicio, LocalDate fim) {
		return new FiltroAtendimentos(null, null, null, null, inicio, fim).comoPredicado();
	}

}
