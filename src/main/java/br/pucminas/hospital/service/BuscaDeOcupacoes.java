package br.pucminas.hospital.service;

import br.pucminas.hospital.model.Periodo;
import java.util.List;

/**
 * Busca o que ocupa um recurso (um profissional, um paciente ou uma sala cirúrgica) perto de um período.
 * Cada recurso é consultado de um jeito, então cada um é representado por uma lambda; a verificação de
 * sobreposição, igual para todos, fica uma única vez em {@link VerificadorDeDisponibilidade}.
 */
@FunctionalInterface
interface BuscaDeOcupacoes {

	/**
	 * @return as ocupações que podem se sobrepor ao período; pode incluir algumas que não se sobrepõem
	 */
	List<Ocupacao> buscar(Periodo periodo);

}
