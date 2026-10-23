package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Paciente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	Optional<Paciente> findByCpf(String cpf);

	boolean existsByCpf(String cpf);

	List<Paciente> findByAtivoTrueOrderByNomeAsc();

}
