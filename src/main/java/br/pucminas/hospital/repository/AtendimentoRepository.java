package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Atendimento;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

	/**
	 * Todos os atendimentos do paciente, de todas as modalidades, em ordem cronológica.
	 */
	// Cada modalidade guarda a sua data em uma coluna de outra tabela (e de tipo diferente), então a
	// ordenação usa o método polimórfico getDataHoraReferencia() em vez de um ORDER BY.
	default List<Atendimento> findHistoricoDoPaciente(Long pacienteId) {
		return findByPacienteId(pacienteId).stream()
				.sorted(Comparator.comparing(Atendimento::getDataHoraReferencia).thenComparing(Atendimento::getId))
				.toList();
	}

	List<Atendimento> findByPacienteId(Long pacienteId);

}
