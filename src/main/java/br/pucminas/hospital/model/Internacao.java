package br.pucminas.hospital.model;

import br.pucminas.hospital.exception.DataInvalidaException;
import br.pucminas.hospital.exception.QuartoSemVagaException;
import br.pucminas.hospital.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "internacao")
public class Internacao extends Atendimento {

	private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quarto_id", nullable = false)
	private Quarto quarto;

	@Column(nullable = false)
	private LocalDate dataEntrada;

	@Column(nullable = false)
	private LocalDate dataPrevistaAlta;

	private LocalDate dataEfetivaAlta;

	protected Internacao() {
	}

	/**
	 * Cria a internação ocupando uma vaga do quarto. A vaga é devolvida em {@link #darAlta(LocalDate)}.
	 *
	 * @throws DataInvalidaException se a alta prevista for anterior à entrada
	 * @throws QuartoSemVagaException se o quarto não tiver vaga
	 */
	public Internacao(Paciente paciente, ProfissionalSaude profissionalResponsavel, Quarto quarto,
			LocalDate dataEntrada, LocalDate dataPrevistaAlta) {
		super(paciente, profissionalResponsavel);
		validarNaoAnteriorAEntrada(dataPrevistaAlta, dataEntrada, "prevista");
		quarto.ocupar();
		this.quarto = quarto;
		this.dataEntrada = dataEntrada;
		this.dataPrevistaAlta = dataPrevistaAlta;
	}

	/**
	 * Encerra a internação e libera a vaga do quarto.
	 *
	 * @throws RegraNegocioException se a internação já estiver encerrada
	 * @throws DataInvalidaException se a data de alta for anterior à entrada
	 */
	public void darAlta(LocalDate dataEfetivaAlta) {
		if (!isAtiva()) {
			throw new RegraNegocioException(
					"Esta internação já foi encerrada em " + FORMATO_DATA.format(this.dataEfetivaAlta) + ".");
		}
		validarNaoAnteriorAEntrada(dataEfetivaAlta, dataEntrada, "efetiva");
		this.dataEfetivaAlta = dataEfetivaAlta;
		quarto.liberar();
	}

	public boolean isAtiva() {
		return dataEfetivaAlta == null;
	}

	private static void validarNaoAnteriorAEntrada(LocalDate dataAlta, LocalDate dataEntrada, String tipoAlta) {
		if (dataAlta.isBefore(dataEntrada)) {
			throw new DataInvalidaException("A data de alta " + tipoAlta + " (" + FORMATO_DATA.format(dataAlta)
					+ ") não pode ser anterior à data de entrada (" + FORMATO_DATA.format(dataEntrada) + ").");
		}
	}

	public Quarto getQuarto() {
		return quarto;
	}

	public LocalDate getDataEntrada() {
		return dataEntrada;
	}

	public LocalDate getDataPrevistaAlta() {
		return dataPrevistaAlta;
	}

	/**
	 * Altera a previsão de alta, que não pode ser anterior à entrada.
	 */
	public void setDataPrevistaAlta(LocalDate dataPrevistaAlta) {
		validarNaoAnteriorAEntrada(dataPrevistaAlta, dataEntrada, "prevista");
		this.dataPrevistaAlta = dataPrevistaAlta;
	}

	public LocalDate getDataEfetivaAlta() {
		return dataEfetivaAlta;
	}

}
