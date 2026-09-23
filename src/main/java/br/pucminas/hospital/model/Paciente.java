package br.pucminas.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "paciente")
public class Paciente extends Pessoa {

	// Somente os 11 dígitos; a validação dos dígitos verificadores fica no DTO de entrada.
	@Column(nullable = false, unique = true, length = 11)
	private String cpf;

	@Column(nullable = false)
	private LocalDate dataNascimento;

	@Embedded
	private Endereco endereco;

	protected Paciente() {
	}

	public Paciente(String nome, String cpf, LocalDate dataNascimento) {
		super(nome);
		this.cpf = cpf;
		this.dataNascimento = dataNascimento;
	}

	public String getCpf() {
		return cpf;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public Endereco getEndereco() {
		return endereco;
	}

	public void setEndereco(Endereco endereco) {
		this.endereco = endereco;
	}

}
