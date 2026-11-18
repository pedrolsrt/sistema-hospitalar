package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.SalaCirurgica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaCirurgicaRepository extends JpaRepository<SalaCirurgica, Long> {

	Optional<SalaCirurgica> findByIdentificacao(String identificacao);

	List<SalaCirurgica> findByAtivaTrueOrderByIdentificacaoAsc();

}
