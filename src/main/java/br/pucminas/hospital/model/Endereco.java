package br.pucminas.hospital.model;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Objeto de valor imutável: para alterar o endereço de um paciente, substitui-se o objeto inteiro.
 */
@Embeddable
public class Endereco {

	private String logradouro;
	private String numero;
	private String complemento;
	private String bairro;
	private String cidade;
	private String uf;
	private String cep;

	protected Endereco() {
	}

	public Endereco(String logradouro, String numero, String complemento, String bairro, String cidade,
			String uf, String cep) {
		this.logradouro = logradouro;
		this.numero = numero;
		this.complemento = complemento;
		this.bairro = bairro;
		this.cidade = cidade;
		this.uf = uf;
		this.cep = cep;
	}

	public String getLogradouro() {
		return logradouro;
	}

	public String getNumero() {
		return numero;
	}

	public String getComplemento() {
		return complemento;
	}

	public String getBairro() {
		return bairro;
	}

	public String getCidade() {
		return cidade;
	}

	public String getUf() {
		return uf;
	}

	public String getCep() {
		return cep;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Endereco outro)) {
			return false;
		}
		return Objects.equals(logradouro, outro.logradouro) && Objects.equals(numero, outro.numero)
				&& Objects.equals(complemento, outro.complemento) && Objects.equals(bairro, outro.bairro)
				&& Objects.equals(cidade, outro.cidade) && Objects.equals(uf, outro.uf)
				&& Objects.equals(cep, outro.cep);
	}

	@Override
	public int hashCode() {
		return Objects.hash(logradouro, numero, complemento, bairro, cidade, uf, cep);
	}

}
