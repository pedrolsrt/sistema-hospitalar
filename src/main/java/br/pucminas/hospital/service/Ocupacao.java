package br.pucminas.hospital.service;

import br.pucminas.hospital.model.Periodo;

/**
 * Atendimento ativo que ocupa um recurso durante um período, por exemplo uma consulta do profissional.
 *
 * @param atendimento nome do atendimento na mensagem de erro, por exemplo "consulta"
 */
record Ocupacao(String atendimento, Periodo periodo) {
}
