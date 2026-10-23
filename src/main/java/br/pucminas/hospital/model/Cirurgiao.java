package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@DiscriminatorValue("CIRURGIAO")
public class Cirurgiao extends ProfissionalSaude {

	// Anulável no banco por causa da tabela única; a obrigatoriedade é garantida aqui e por CHECK na V3.
	@Enumerated(EnumType.STRING)
	@Column(name = "area_cirurgica", length = 30)
	private AreaAtuacao areaCirurgica;

	protected Cirurgiao() {
	}

	/**
	 * @throws RegraNegocioException se a área cirúrgica não for informada
	 */
	public Cirurgiao(String nome, String registroProfissional, AreaAtuacao areaCirurgica) {
		super(nome, registroProfissional);
		setAreaCirurgica(areaCirurgica);
	}

	@Override
	public boolean atendeArea(AreaAtuacao area) {
		return areaCirurgica == area;
	}

	@Override
	public boolean podeRealizarProcedimentos() {
		return true;
	}

	public AreaAtuacao getAreaCirurgica() {
		return areaCirurgica;
	}

	/**
	 * @throws RegraNegocioException se a área cirúrgica não for informada
	 */
	public void setAreaCirurgica(AreaAtuacao areaCirurgica) {
		if (areaCirurgica == null) {
			throw new RegraNegocioException("A área cirúrgica do cirurgião é obrigatória.");
		}
		this.areaCirurgica = areaCirurgica;
	}

}
