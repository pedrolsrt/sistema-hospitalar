package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.Periodo;
import br.pucminas.hospital.model.SituacaoConsulta;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

	/**
	 * Todas as consultas, para a listagem filtrada em memória. Paciente e profissional vêm na mesma
	 * consulta, porque toda resposta mostra os dois.
	 */
	@Override
	@EntityGraph(attributePaths = { "paciente", "profissionalResponsavel" })
	List<Consulta> findAll();

	/**
	 * Consultas ativas do profissional que podem se sobrepor ao período. A sobreposição exata é decidida
	 * depois, com {@link Periodo#sobrepoe}.
	 */
	default List<Consulta> findAtivasDoProfissionalProximasDe(Long profissionalId, Periodo periodo) {
		return findByProfissionalResponsavelIdAndDataBetweenAndSituacaoIn(profissionalId, primeiraData(periodo),
				periodo.fim().toLocalDate(), SituacaoConsulta.ativas());
	}

	/**
	 * Consultas ativas do paciente que podem se sobrepor ao período. A sobreposição exata é decidida
	 * depois, com {@link Periodo#sobrepoe}.
	 */
	default List<Consulta> findAtivasDoPacienteProximasDe(Long pacienteId, Periodo periodo) {
		return findByPacienteIdAndDataBetweenAndSituacaoIn(pacienteId, primeiraData(periodo),
				periodo.fim().toLocalDate(), SituacaoConsulta.ativas());
	}

	// A consulta é gravada por data e horário e dura menos de um dia: além das datas do período, só uma
	// consulta do dia anterior, que passe da meia-noite, pode alcançá-lo.
	private static LocalDate primeiraData(Periodo periodo) {
		return periodo.inicio().toLocalDate().minusDays(1);
	}

	default boolean existeAtivaDoPaciente(Long pacienteId) {
		return existsByPacienteIdAndSituacaoIn(pacienteId, SituacaoConsulta.ativas());
	}

	default boolean existeAtivaDoProfissional(Long profissionalId) {
		return existsByProfissionalResponsavelIdAndSituacaoIn(profissionalId, SituacaoConsulta.ativas());
	}

	boolean existsByPacienteIdAndSituacaoIn(Long pacienteId, Collection<SituacaoConsulta> situacoes);

	boolean existsByProfissionalResponsavelIdAndSituacaoIn(Long profissionalId, Collection<SituacaoConsulta> situacoes);

	List<Consulta> findByProfissionalResponsavelIdAndDataBetweenAndSituacaoIn(Long profissionalId, LocalDate de,
			LocalDate ate, Collection<SituacaoConsulta> situacoes);

	List<Consulta> findByPacienteIdAndDataBetweenAndSituacaoIn(Long pacienteId, LocalDate de, LocalDate ate,
			Collection<SituacaoConsulta> situacoes);

}
