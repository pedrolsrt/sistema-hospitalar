package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Paciente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	boolean existsByCpf(String cpf);

	List<Paciente> findByAtivoTrueOrderByNomeAsc();

}
