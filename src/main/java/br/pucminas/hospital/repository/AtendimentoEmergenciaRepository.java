package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.SituacaoAtendimento;
import br.pucminas.hospital.model.SituacaoEmergencia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtendimentoEmergenciaRepository extends JpaRepository<AtendimentoEmergencia, Long> {

	List<AtendimentoEmergencia> findBySituacaoOrderByDataHoraChegadaAsc(SituacaoEmergencia situacao);

	default boolean existeAtivoDoPaciente(Long pacienteId) {
		return existsByPacienteIdAndSituacaoIn(pacienteId, SituacaoAtendimento.ativas(SituacaoEmergencia.class));
	}

	default boolean existeAtivoDoProfissional(Long profissionalId) {
		return existsByProfissionalResponsavelIdAndSituacaoIn(profissionalId,
				SituacaoAtendimento.ativas(SituacaoEmergencia.class));
	}

	boolean existsByPacienteIdAndSituacaoIn(Long pacienteId, Collection<SituacaoEmergencia> situacoes);

	boolean existsByProfissionalResponsavelIdAndSituacaoIn(Long profissionalId,
			Collection<SituacaoEmergencia> situacoes);

}
