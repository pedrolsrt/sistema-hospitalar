package br.pucminas.hospital.model;

/**
 * Categorias de profissional de saúde. Cada constante sabe criar o profissional da sua categoria,
 * o que evita decidir a subclasse com condicionais fora daqui.
 */
public enum CategoriaProfissional {

	CLINICO_GERAL("Clínico geral", false) {
		@Override
		public ProfissionalSaude criar(String nome, String registroProfissional, AreaAtuacao area) {
			return new ClinicoGeral(nome, registroProfissional);
		}
	},

	MEDICO_ESPECIALISTA("Médico especialista", true) {
		@Override
		public ProfissionalSaude criar(String nome, String registroProfissional, AreaAtuacao area) {
			return new MedicoEspecialista(nome, registroProfissional, area);
		}
	},

	CIRURGIAO("Cirurgião", true) {
		@Override
		public ProfissionalSaude criar(String nome, String registroProfissional, AreaAtuacao area) {
			return new Cirurgiao(nome, registroProfissional, area);
		}
	};

	private final String descricao;
	private final boolean exigeArea;

	CategoriaProfissional(String descricao, boolean exigeArea) {
		this.descricao = descricao;
		this.exigeArea = exigeArea;
	}

	/**
	 * Cria o profissional da categoria. A área é usada apenas pelas categorias que a exigem.
	 */
	public abstract ProfissionalSaude criar(String nome, String registroProfissional, AreaAtuacao area);

	public boolean exigeArea() {
		return exigeArea;
	}

	public String getDescricao() {
		return descricao;
	}

}
