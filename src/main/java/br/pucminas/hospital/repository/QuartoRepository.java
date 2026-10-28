package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoQuarto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuartoRepository extends JpaRepository<Quarto, Long> {

	Optional<Quarto> findByNumero(String numero);

	/**
	 * Quartos em ordem de número, opcionalmente só de um tipo (nulo = todos) e só os com vaga.
	 */
	@Query("""
			select q from Quarto q
			where (:tipo is null or q.tipo = :tipo)
			  and (:somenteComVaga = false or q.ocupacaoAtual < q.capacidadeMaxima)
			order by q.numero
			""")
	List<Quarto> findComFiltros(@Param("tipo") TipoQuarto tipo, @Param("somenteComVaga") boolean somenteComVaga);

}
