package br.pucminas.hospital.service;

import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.repository.PacienteRepository;
import br.pucminas.hospital.repository.ProfissionalSaudeRepository;
import br.pucminas.hospital.repository.QuartoRepository;

/**
 * Busca por id das entidades usadas por mais de um service, com a mesma mensagem de "não encontrado".
 */
final class Busca {

	private Busca() {
	}

	static Paciente paciente(PacienteRepository repository, Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Paciente não encontrado (id " + id + ")."));
	}

	static ProfissionalSaude profissional(ProfissionalSaudeRepository repository, Long id) {
		return repository.findById(id).orElseThrow(
				() -> new RecursoNaoEncontradoException("Profissional de saúde não encontrado (id " + id + ")."));
	}

	static Quarto quarto(QuartoRepository repository, Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Quarto não encontrado (id " + id + ")."));
	}

}
