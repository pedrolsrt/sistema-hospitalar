package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.SituacaoConsulta;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

	/**
	 * Consultas ativas (que ocupam horário) do profissional na data, para a regra de horários sobrepostos.
	 */
	default List<Consulta> findAtivasDoProfissionalNaData(Long profissionalId, LocalDate data) {
		return findByProfissionalResponsavelIdAndDataAndSituacaoIn(profissionalId, data, SituacaoConsulta.ativas());
	}

	/**
	 * Consultas ativas (que ocupam horário) do paciente na data, para a regra de horários sobrepostos.
	 */
	default List<Consulta> findAtivasDoPacienteNaData(Long pacienteId, LocalDate data) {
		return findByPacienteIdAndDataAndSituacaoIn(pacienteId, data, SituacaoConsulta.ativas());
	}

	default boolean existeAtivaDoPaciente(Long pacienteId) {
		return existsByPacienteIdAndSituacaoIn(pacienteId, SituacaoConsulta.ativas());
	}

	default boolean existeAtivaDoProfissional(Long profissionalId) {
		return existsByProfissionalResponsavelIdAndSituacaoIn(profissionalId, SituacaoConsulta.ativas());
	}

	boolean existsByPacienteIdAndSituacaoIn(Long pacienteId, Collection<SituacaoConsulta> situacoes);

	boolean existsByProfissionalResponsavelIdAndSituacaoIn(Long profissionalId, Collection<SituacaoConsulta> situacoes);

	List<Consulta> findByProfissionalResponsavelIdAndDataAndSituacaoIn(Long profissionalId, LocalDate data,
			Collection<SituacaoConsulta> situacoes);

	List<Consulta> findByPacienteIdAndDataAndSituacaoIn(Long pacienteId, LocalDate data,
			Collection<SituacaoConsulta> situacoes);

	/**
	 * Agenda do profissional na data, em qualquer situação, em ordem de horário.
	 */
	@Query("""
			select c from Consulta c
			where c.profissionalResponsavel.id = :profissionalId and c.data = :data
			order by c.horario
			""")
	List<Consulta> findAgendaDoProfissional(@Param("profissionalId") Long profissionalId,
			@Param("data") LocalDate data);

	/**
	 * Consultas com filtros opcionais (nulo = sem filtro), em ordem de data e horário.
	 */
	@Query("""
			select c from Consulta c
			where (:situacao is null or c.situacao = :situacao)
			  and (:profissionalId is null or c.profissionalResponsavel.id = :profissionalId)
			  and (:data is null or c.data = :data)
			order by c.data, c.horario
			""")
	List<Consulta> findComFiltros(@Param("situacao") SituacaoConsulta situacao,
			@Param("profissionalId") Long profissionalId, @Param("data") LocalDate data);

}
