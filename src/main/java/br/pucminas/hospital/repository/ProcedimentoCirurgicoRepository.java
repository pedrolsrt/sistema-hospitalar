package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Periodo;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.model.SituacaoAtendimento;
import br.pucminas.hospital.model.SituacaoProcedimento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcedimentoCirurgicoRepository extends JpaRepository<ProcedimentoCirurgico, Long> {

	/**
	 * Procedimentos realizados (concluídos) pelo cirurgião, do mais recente para o mais antigo. Substitui
	 * uma coleção mapeada em Cirurgiao, que traria também os cancelados e cresceria sem limite.
	 */
	@Query("""
			select p from ProcedimentoCirurgico p
			where p.profissionalResponsavel.id = :cirurgiaoId
			  and p.situacao = br.pucminas.hospital.model.SituacaoProcedimento.CONCLUIDO
			order by p.dataHoraFim desc
			""")
	List<ProcedimentoCirurgico> findConcluidosDoCirurgiao(@Param("cirurgiaoId") Long cirurgiaoId);

	/**
	 * Procedimentos previstos para a data, em qualquer situação, em ordem de horário.
	 */
	default List<ProcedimentoCirurgico> findPrevistosNaData(LocalDate data) {
		return findPrevistosEntre(data.atStartOfDay(), data.plusDays(1).atStartOfDay());
	}

	// Intervalo semiaberto [início, fim): compara a data e hora sem converter a coluna.
	@Query("""
			select p from ProcedimentoCirurgico p
			where p.dataHoraPrevista >= :inicio and p.dataHoraPrevista < :fim
			order by p.dataHoraPrevista
			""")
	List<ProcedimentoCirurgico> findPrevistosEntre(@Param("inicio") LocalDateTime inicio,
			@Param("fim") LocalDateTime fim);

	/**
	 * Procedimentos ativos do profissional que podem se sobrepor ao período. A sobreposição exata é decidida
	 * depois, com {@link Periodo#sobrepoe}.
	 */
	default List<ProcedimentoCirurgico> findAtivosDoProfissionalProximosDe(Long profissionalId, Periodo periodo) {
		return findByProfissionalResponsavelIdAndSituacaoInAndDataHoraPrevistaBetween(profissionalId,
				SituacaoAtendimento.ativas(SituacaoProcedimento.class), inicioMaisAntigo(periodo), periodo.fim());
	}

	/**
	 * Procedimentos ativos do paciente que podem se sobrepor ao período. A sobreposição exata é decidida
	 * depois, com {@link Periodo#sobrepoe}.
	 */
	default List<ProcedimentoCirurgico> findAtivosDoPacienteProximosDe(Long pacienteId, Periodo periodo) {
		return findByPacienteIdAndSituacaoInAndDataHoraPrevistaBetween(pacienteId,
				SituacaoAtendimento.ativas(SituacaoProcedimento.class), inicioMaisAntigo(periodo), periodo.fim());
	}

	/**
	 * Procedimentos ativos na sala que podem se sobrepor ao período. A sobreposição exata é decidida
	 * depois, com {@link Periodo#sobrepoe}.
	 */
	default List<ProcedimentoCirurgico> findAtivosDaSalaProximosDe(Long salaCirurgicaId, Periodo periodo) {
		return findBySalaCirurgicaIdAndSituacaoInAndDataHoraPrevistaBetween(salaCirurgicaId,
				SituacaoAtendimento.ativas(SituacaoProcedimento.class), inicioMaisAntigo(periodo), periodo.fim());
	}

	// O fim previsto não é uma coluna: um procedimento que começou mais de uma duração máxima antes do
	// período já terminou antes dele, então basta buscar pelo início previsto.
	private static LocalDateTime inicioMaisAntigo(Periodo periodo) {
		return periodo.inicio().minusMinutes(ProcedimentoCirurgico.DURACAO_MAXIMA_MINUTOS);
	}

	List<ProcedimentoCirurgico> findByProfissionalResponsavelIdAndSituacaoInAndDataHoraPrevistaBetween(
			Long profissionalId, Collection<SituacaoProcedimento> situacoes, LocalDateTime de, LocalDateTime ate);

	List<ProcedimentoCirurgico> findByPacienteIdAndSituacaoInAndDataHoraPrevistaBetween(Long pacienteId,
			Collection<SituacaoProcedimento> situacoes, LocalDateTime de, LocalDateTime ate);

	List<ProcedimentoCirurgico> findBySalaCirurgicaIdAndSituacaoInAndDataHoraPrevistaBetween(Long salaCirurgicaId,
			Collection<SituacaoProcedimento> situacoes, LocalDateTime de, LocalDateTime ate);

	default boolean existeAtivoNaSala(Long salaCirurgicaId) {
		return existsBySalaCirurgicaIdAndSituacaoIn(salaCirurgicaId,
				SituacaoAtendimento.ativas(SituacaoProcedimento.class));
	}

	boolean existsBySalaCirurgicaIdAndSituacaoIn(Long salaCirurgicaId, Collection<SituacaoProcedimento> situacoes);

	default boolean existeAtivoDoPaciente(Long pacienteId) {
		return existsByPacienteIdAndSituacaoIn(pacienteId, SituacaoAtendimento.ativas(SituacaoProcedimento.class));
	}

	default boolean existeAtivoDoProfissional(Long profissionalId) {
		return existsByProfissionalResponsavelIdAndSituacaoIn(profissionalId,
				SituacaoAtendimento.ativas(SituacaoProcedimento.class));
	}

	boolean existsByPacienteIdAndSituacaoIn(Long pacienteId, Collection<SituacaoProcedimento> situacoes);

	boolean existsByProfissionalResponsavelIdAndSituacaoIn(Long profissionalId,
			Collection<SituacaoProcedimento> situacoes);

}
