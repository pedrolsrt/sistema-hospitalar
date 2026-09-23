package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.QuartoSemVagaException;
import br.pucminas.hospital.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;

@Entity
@Table(name = "quarto")
public class Quarto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String numero;

	@Column(nullable = false)
	private int andar;

	@Column(nullable = false)
	private int capacidadeMaxima;

	@Column(nullable = false)
	private int ocupacaoAtual;

	// Duas internações simultâneas no mesmo quarto não podem ultrapassar a capacidade: a segunda
	// transação a gravar falha por conflito de versão em vez de sobrescrever a ocupação.
	@Version
	private Long versao;

	protected Quarto() {
	}

	public Quarto(String numero, int andar, int capacidadeMaxima) {
		validarCapacidade(capacidadeMaxima, 0);
		this.numero = numero;
		this.andar = andar;
		this.capacidadeMaxima = capacidadeMaxima;
	}

	/**
	 * Ocupa uma vaga do quarto.
	 *
	 * @throws QuartoSemVagaException se o quarto já estiver na capacidade máxima
	 */
	public void ocupar() {
		if (!temVaga()) {
			throw new QuartoSemVagaException(numero);
		}
		ocupacaoAtual++;
	}

	/**
	 * Libera uma vaga do quarto.
	 *
	 * @throws RegraNegocioException se o quarto já estiver vazio
	 */
	public void liberar() {
		if (ocupacaoAtual == 0) {
			throw new RegraNegocioException("O quarto " + numero + " não possui pacientes internados.");
		}
		ocupacaoAtual--;
	}

	public boolean temVaga() {
		return ocupacaoAtual < capacidadeMaxima;
	}

	public SituacaoQuarto getSituacao() {
		return temVaga() ? SituacaoQuarto.DISPONIVEL : SituacaoQuarto.OCUPADO;
	}

	private static void validarCapacidade(int capacidadeMaxima, int ocupacaoAtual) {
		if (capacidadeMaxima <= 0) {
			throw new RegraNegocioException("A capacidade máxima do quarto deve ser maior que zero.");
		}
		if (capacidadeMaxima < ocupacaoAtual) {
			throw new RegraNegocioException("A capacidade máxima não pode ser menor que a ocupação atual do quarto ("
					+ ocupacaoAtual + ").");
		}
	}

	public Long getId() {
		return id;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public int getAndar() {
		return andar;
	}

	public void setAndar(int andar) {
		this.andar = andar;
	}

	public int getCapacidadeMaxima() {
		return capacidadeMaxima;
	}

	/**
	 * Altera a capacidade, que precisa ser positiva e comportar os pacientes já internados.
	 */
	public void setCapacidadeMaxima(int capacidadeMaxima) {
		validarCapacidade(capacidadeMaxima, ocupacaoAtual);
		this.capacidadeMaxima = capacidadeMaxima;
	}

	public int getOcupacaoAtual() {
		return ocupacaoAtual;
	}

	public Long getVersao() {
		return versao;
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}
		Quarto outro = (Quarto) o;
		return id != null && id.equals(outro.getId());
	}

	@Override
	public final int hashCode() {
		return Hibernate.getClass(this).hashCode();
	}

}
