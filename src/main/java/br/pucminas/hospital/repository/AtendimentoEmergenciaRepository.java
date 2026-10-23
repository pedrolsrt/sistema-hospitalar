package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.SituacaoEmergencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AtendimentoEmergenciaRepository extends JpaRepository<AtendimentoEmergencia, Long> {

	/**
	 * Fila de quem aguarda avaliação: prioridade mais grave primeiro, depois ordem de chegada. Quem ainda
	 * não tem prioridade definida fica no fim da fila.
	 */
	// Os pesos do CASE repetem PrioridadeEmergencia.getNivel(); o teste do repository deriva a ordem
	// esperada dos níveis do enum, para acusar qualquer divergência.
	@Query("""
			select e from AtendimentoEmergencia e
			where e.situacao = br.pucminas.hospital.model.SituacaoEmergencia.AGUARDANDO_AVALIACAO
			order by case e.prioridade
			           when br.pucminas.hospital.model.PrioridadeEmergencia.CRITICA then 4
			           when br.pucminas.hospital.model.PrioridadeEmergencia.ALTA then 3
			           when br.pucminas.hospital.model.PrioridadeEmergencia.MODERADA then 2
			           when br.pucminas.hospital.model.PrioridadeEmergencia.BAIXA then 1
			           else 0
			         end desc,
			         e.dataHoraChegada
			""")
	List<AtendimentoEmergencia> findFilaDeAvaliacao();

	List<AtendimentoEmergencia> findBySituacaoOrderByDataHoraChegadaAsc(SituacaoEmergencia situacao);

}
