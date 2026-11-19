package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Quarto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuartoRepository extends JpaRepository<Quarto, Long> {

	Optional<Quarto> findByNumero(String numero);

}
