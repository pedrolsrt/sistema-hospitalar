package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@DiscriminatorValue("MEDICO_ESPECIALISTA")
public class MedicoEspecialista extends ProfissionalSaude {

	// Anulável no banco por causa da tabela única; a obrigatoriedade é garantida aqui e por CHECK na V3.
	@Enumerated(EnumType.STRING)
	@Column(name = "area_atuacao", length = 30)
	private AreaAtuacao areaAtuacao;

	protected MedicoEspecialista() {
	}

	/**
	 * @throws RegraNegocioException se a área de atuação não for informada
	 */
	public MedicoEspecialista(String nome, String registroProfissional, AreaAtuacao areaAtuacao) {
		super(nome, registroProfissional);
		setAreaAtuacao(areaAtuacao);
	}

	@Override
	public boolean atendeArea(AreaAtuacao area) {
		return areaAtuacao == area;
	}

	@Override
	public boolean podeRealizarConsultaEspecializada(AreaAtuacao area) {
		return atendeArea(area);
	}

	public AreaAtuacao getAreaAtuacao() {
		return areaAtuacao;
	}

	/**
	 * @throws RegraNegocioException se a área de atuação não for informada
	 */
	public void setAreaAtuacao(AreaAtuacao areaAtuacao) {
		if (areaAtuacao == null) {
			throw new RegraNegocioException("A área de atuação do médico especialista é obrigatória.");
		}
		this.areaAtuacao = areaAtuacao;
	}

}
