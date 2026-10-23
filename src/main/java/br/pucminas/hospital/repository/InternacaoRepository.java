package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Internacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Internação ativa é a que ainda não tem data efetiva de alta.
public interface InternacaoRepository extends JpaRepository<Internacao, Long> {

	List<Internacao> findByDataEfetivaAltaIsNullOrderByDataEntradaAsc();

	Optional<Internacao> findByPacienteIdAndDataEfetivaAltaIsNull(Long pacienteId);

	boolean existsByQuartoIdAndDataEfetivaAltaIsNull(Long quartoId);

	boolean existsByQuartoId(Long quartoId);

}
