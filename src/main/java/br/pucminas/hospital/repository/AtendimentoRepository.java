package br.pucminas.hospital.repository;

import br.pucminas.hospital.model.Atendimento;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

// A ordenação fica no service, com OrdenacaoAtendimentos: cada modalidade guarda a sua data em uma coluna
// de outra tabela (e de tipo diferente), então não há um ORDER BY comum a todas.
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

	/**
	 * Todos os atendimentos, de todas as modalidades, para a busca filtrada em memória. Paciente e profissional
	 * vêm na mesma consulta, porque toda resposta mostra os dois.
	 */
	@Override
	@EntityGraph(attributePaths = { "paciente", "profissionalResponsavel" })
	List<Atendimento> findAll();

	List<Atendimento> findByPacienteId(Long pacienteId);

}
