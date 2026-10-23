package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuartoRepository extends JpaRepository<Quarto, Long> {

	Optional<Quarto> findByNumero(String numero);

	@Query("select q from Quarto q where q.ocupacaoAtual < q.capacidadeMaxima order by q.numero")
	List<Quarto> findComVaga();

	List<Quarto> findByTipoOrderByNumeroAsc(TipoQuarto tipo);

}
