package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.HistoricoMedicoResponse;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.mapper.HistoricoMedicoMapper;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.repository.AtendimentoRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Histórico médico do paciente. Não é uma entidade: é montado a partir dos atendimentos de todas as
 * modalidades.
 */
@Service
@Transactional(readOnly = true)
public class HistoricoMedicoService {

	private final PacienteRepository pacienteRepository;
	private final AtendimentoRepository atendimentoRepository;
	private final HistoricoMedicoMapper mapper;

	public HistoricoMedicoService(PacienteRepository pacienteRepository, AtendimentoRepository atendimentoRepository,
			HistoricoMedicoMapper mapper) {
		this.pacienteRepository = pacienteRepository;
		this.atendimentoRepository = atendimentoRepository;
		this.mapper = mapper;
	}

	/**
	 * Todos os atendimentos do paciente em ordem cronológica, com a modalidade e os dados de cada um.
	 * Inclui os atendimentos cancelados ou não realizados, que também fazem parte do histórico.
	 *
	 * @throws RecursoNaoEncontradoException se o paciente não existir
	 */
	public HistoricoMedicoResponse montar(Long pacienteId) {
		Paciente paciente = Busca.paciente(pacienteRepository, pacienteId);
		return mapper.paraResponse(paciente, atendimentoRepository.findHistoricoDoPaciente(pacienteId));
	}

}
