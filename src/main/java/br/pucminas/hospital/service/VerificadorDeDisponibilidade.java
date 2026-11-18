package br.pucminas.hospital.service;

import br.pucminas.hospital.exception.AtendimentoIndisponivelException;
import br.pucminas.hospital.model.Consulta;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.Periodo;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.model.ProfissionalSaude;
import br.pucminas.hospital.model.SalaCirurgica;
import br.pucminas.hospital.repository.ConsultaRepository;
import br.pucminas.hospital.repository.ProcedimentoCirurgicoRepository;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Disponibilidade de horário no agendamento de consultas e procedimentos cirúrgicos, as modalidades com
 * horário marcado. Só os atendimentos em situação ativa ocupam horário; emergências e internações ficam de fora.
 */
@Component
class VerificadorDeDisponibilidade {

	private final ConsultaRepository consultaRepository;
	private final ProcedimentoCirurgicoRepository procedimentoRepository;
	private final Duration duracaoConsulta;

	VerificadorDeDisponibilidade(ConsultaRepository consultaRepository,
			ProcedimentoCirurgicoRepository procedimentoRepository,
			@Value("${hospital.consulta.duracao-minutos}") long duracaoConsultaMinutos) {
		this.consultaRepository = consultaRepository;
		this.procedimentoRepository = procedimentoRepository;
		this.duracaoConsulta = Duration.ofMinutes(duracaoConsultaMinutos);
	}

	/**
	 * Exige que o profissional e o paciente da consulta não tenham consulta nem procedimento no período.
	 *
	 * @throws AtendimentoIndisponivelException se o período estiver ocupado
	 */
	void exigirHorarioLivre(Consulta consulta) {
		Periodo pedido = consulta.getPeriodoPrevisto(duracaoConsulta);
		exigirLivre(doProfissional(consulta.getProfissionalResponsavel()), pedido,
				ocupacoesDoProfissional(consulta.getProfissionalResponsavel()));
		exigirLivre(doPaciente(consulta.getPaciente()), pedido, ocupacoesDoPaciente(consulta.getPaciente()));
	}

	/**
	 * Exige que o cirurgião e o paciente não tenham consulta nem procedimento no período e que a sala não
	 * tenha outro procedimento nele.
	 *
	 * @throws AtendimentoIndisponivelException se o período estiver ocupado
	 */
	void exigirHorarioLivre(ProcedimentoCirurgico procedimento) {
		Periodo pedido = procedimento.getPeriodoPrevisto();
		exigirLivre(doProfissional(procedimento.getProfissionalResponsavel()), pedido,
				ocupacoesDoProfissional(procedimento.getProfissionalResponsavel()));
		exigirLivre(doPaciente(procedimento.getPaciente()), pedido, ocupacoesDoPaciente(procedimento.getPaciente()));
		SalaCirurgica sala = procedimento.getSalaCirurgica();
		exigirLivre("A sala cirúrgica " + sala.getIdentificacao(), pedido, ocupacoesDaSala(sala));
	}

	// Único ponto com a regra de sobreposição, qualquer que seja o recurso.
	private static void exigirLivre(String recurso, Periodo pedido, BuscaDeOcupacoes busca) {
		busca.buscar(pedido).stream()
				.filter(ocupacao -> ocupacao.periodo().sobrepoe(pedido))
				.findFirst()
				.ifPresent(ocupacao -> {
					throw new AtendimentoIndisponivelException(recurso + " já tem " + ocupacao.atendimento() + " em "
							+ ocupacao.periodo().descricao() + ", que se sobrepõe ao período pedido ("
							+ pedido.descricao() + ").");
				});
	}

	private BuscaDeOcupacoes ocupacoesDoProfissional(ProfissionalSaude profissional) {
		return periodo -> juntar(consultaRepository.findAtivasDoProfissionalProximasDe(profissional.getId(), periodo),
				procedimentoRepository.findAtivosDoProfissionalProximosDe(profissional.getId(), periodo));
	}

	private BuscaDeOcupacoes ocupacoesDoPaciente(Paciente paciente) {
		return periodo -> juntar(consultaRepository.findAtivasDoPacienteProximasDe(paciente.getId(), periodo),
				procedimentoRepository.findAtivosDoPacienteProximosDe(paciente.getId(), periodo));
	}

	private BuscaDeOcupacoes ocupacoesDaSala(SalaCirurgica sala) {
		return periodo -> procedimentoRepository.findAtivosDaSalaProximosDe(sala.getId(), periodo).stream()
				.map(VerificadorDeDisponibilidade::ocupacaoDoProcedimento)
				.toList();
	}

	private List<Ocupacao> juntar(List<Consulta> consultas, List<ProcedimentoCirurgico> procedimentos) {
		return Stream.concat(consultas.stream().map(this::ocupacaoDaConsulta),
				procedimentos.stream().map(VerificadorDeDisponibilidade::ocupacaoDoProcedimento)).toList();
	}

	private Ocupacao ocupacaoDaConsulta(Consulta consulta) {
		return new Ocupacao("consulta", consulta.getPeriodoPrevisto(duracaoConsulta));
	}

	private static Ocupacao ocupacaoDoProcedimento(ProcedimentoCirurgico procedimento) {
		return new Ocupacao("procedimento cirúrgico", procedimento.getPeriodoPrevisto());
	}

	private static String doProfissional(ProfissionalSaude profissional) {
		return "O profissional " + profissional.getNome();
	}

	private static String doPaciente(Paciente paciente) {
		return "O paciente " + paciente.getNome();
	}

}
