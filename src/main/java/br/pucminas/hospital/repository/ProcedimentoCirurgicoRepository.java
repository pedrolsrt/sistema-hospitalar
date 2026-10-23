package br.pucminas.hospital.repository;

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
	 * uma coleção mapeada em Cirurgiao (ver CLAUDE.md).
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

	// Intervalo semiaberto [inicio, fim): compara a data e hora sem converter a coluna.
	@Query("""
			select p from ProcedimentoCirurgico p
			where p.dataHoraPrevista >= :inicio and p.dataHoraPrevista < :fim
			order by p.dataHoraPrevista
			""")
	List<ProcedimentoCirurgico> findPrevistosEntre(@Param("inicio") LocalDateTime inicio,
			@Param("fim") LocalDateTime fim);

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
