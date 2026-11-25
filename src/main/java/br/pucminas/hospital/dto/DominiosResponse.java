package br.pucminas.hospital.dto;

import java.util.List;

/**
 * Valores aceitos pela API em cada enum, na ordem em que foram declarados, para as telas montarem as listas de
 * opções sem repetir os códigos e as descrições. As categorias de profissional trazem também se exigem área de
 * atuação, e os tipos de quarto, o limite de leitos.
 */
public record DominiosResponse(
		List<CodigoDescricao> situacoesConsulta,
		List<CodigoDescricao> situacoesEmergencia,
		List<CodigoDescricao> situacoesProcedimento,
		List<CodigoDescricao> situacoesInternacao,
		List<CodigoDescricao> modalidades,
		List<CategoriaProfissionalResponse> categoriasProfissional,
		List<CodigoDescricao> areasAtuacao,
		List<TipoQuartoResponse> tiposQuarto,
		List<CodigoDescricao> prioridadesEmergencia,
		List<CodigoDescricao> tiposCirurgia,
		List<CodigoDescricao> ordenacoes) {
}
