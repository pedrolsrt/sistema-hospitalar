package br.pucminas.hospital.model;

public enum ModalidadeAtendimento implements Descritivel {
	CONSULTA_ROTINA("Consulta de rotina"),
	CONSULTA_ESPECIALIZADA("Consulta especializada"),
	EMERGENCIA("Atendimento de emergência"),
	PROCEDIMENTO_CIRURGICO("Procedimento cirúrgico"),
	INTERNACAO("Internação");

	private final String descricao;

	ModalidadeAtendimento(String descricao) {
		this.descricao = descricao;
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

}
