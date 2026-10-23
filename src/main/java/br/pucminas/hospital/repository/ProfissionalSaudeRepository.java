package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.ProfissionalSaude;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfissionalSaudeRepository extends JpaRepository<ProfissionalSaude, Long> {

	boolean existsByRegistroProfissional(String registroProfissional);

	List<ProfissionalSaude> findByAtivoTrueOrderByNomeAsc();

	/**
	 * Profissionais ativos que atuam na área: especialistas pela área de atuação e cirurgiões pela área
	 * cirúrgica. É o mesmo critério de {@link ProfissionalSaude#atendeArea}, aplicado no banco.
	 */
	@Query("""
			select p from ProfissionalSaude p
			where p.ativo = true
			  and (treat(p as MedicoEspecialista).areaAtuacao = :area
			       or treat(p as Cirurgiao).areaCirurgica = :area)
			order by p.nome
			""")
	List<ProfissionalSaude> findAtivosPorArea(@Param("area") AreaAtuacao area);

}
