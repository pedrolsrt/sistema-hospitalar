package br.pucminas.hospital.service;

import br.pucminas.hospital.dto.AtendimentoItemResponse;
import br.pucminas.hospital.mapper.AtendimentoMapper;
import br.pucminas.hospital.model.FiltroAtendimentos;
import br.pucminas.hospital.model.OrdenacaoAtendimentos;
import br.pucminas.hospital.repository.AtendimentoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca de atendimentos de todas as modalidades, filtrados e ordenados em memória: o filtro é o
 * {@link java.util.function.Predicate} montado por {@link FiltroAtendimentos} e a ordem é o
 * {@link java.util.Comparator} da {@link OrdenacaoAtendimentos} escolhida.
 */
@Service
@Transactional(readOnly = true)
public class AtendimentoService {

	private final AtendimentoRepository repository;
	private final AtendimentoMapper mapper;

	public AtendimentoService(AtendimentoRepository repository, AtendimentoMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	public List<AtendimentoItemResponse> listar(FiltroAtendimentos filtro, OrdenacaoAtendimentos ordenacao) {
		return repository.findAll().stream()
				.filter(filtro.comoPredicado())
				.sorted(ordenacao.comparador())
				.map(mapper::paraItem)
				.toList();
	}

}
