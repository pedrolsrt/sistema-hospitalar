package br.pucminas.hospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Aplica a V6 sobre quartos cadastrados antes da existência de tipos. Cada teste usa um H2 próprio,
 * separado do banco do contexto Spring, para poder parar a migração na V5.
 */
class MigracaoV6Test {

	private static final int VIOLACAO_CHECK = 23513;

	// Uma única conexão para o Flyway e para o teste; o motivo está em MigracaoV3Test.
	private SingleConnectionDataSource banco;

	@BeforeEach
	void criarBancoNaV5() {
		String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
		banco = new SingleConnectionDataSource(url, "sa", "", true);
		migrarAte("5");
	}

	@AfterEach
	void fecharBanco() {
		banco.destroy();
	}

	private void migrarAte(String versao) {
		Flyway.configure().dataSource(banco).target(versao).load().migrate();
	}

	private void executar(String sql) throws SQLException {
		try (Connection conexao = banco.getConnection(); Statement comando = conexao.createStatement()) {
			comando.executeUpdate(sql);
		}
	}

	private Map<String, String> tipoPorNumero() throws SQLException {
		Map<String, String> tipos = new HashMap<>();
		try (Connection conexao = banco.getConnection();
				Statement comando = conexao.createStatement();
				ResultSet resultado = comando.executeQuery("SELECT numero, tipo FROM quarto")) {
			while (resultado.next()) {
				tipos.put(resultado.getString("numero"), resultado.getString("tipo"));
			}
		}
		return tipos;
	}

	@Test
	void quartosExistentesPassamASerEnfermarias() throws SQLException {
		executar("INSERT INTO quarto (numero, andar, capacidade_maxima) VALUES ('101', 1, 6)");
		executar("INSERT INTO quarto (numero, andar, capacidade_maxima) VALUES ('102', 1, 1)");

		migrarAte("6");

		assertThat(tipoPorNumero()).containsOnly(Map.entry("101", "ENFERMARIA"), Map.entry("102", "ENFERMARIA"));
	}

	@Test
	void falhaComMensagemClaraSeUmQuartoNaoCabeEmEnfermaria() throws SQLException {
		executar("INSERT INTO quarto (numero, andar, capacidade_maxima) VALUES ('101', 1, 6)");
		executar("INSERT INTO quarto (numero, andar, capacidade_maxima) VALUES ('305', 3, 8)");

		assertThatThrownBy(() -> migrarAte("6"))
				.isInstanceOf(FlywayException.class)
				.hasMessageContaining("Migração V6 interrompida: o quarto 305 tem capacidade 8, acima do limite de 6 "
						+ "leitos da enfermaria. Ajuste a capacidade antes de migrar.");
		// A verificação é o primeiro comando da V6: nada do schema foi alterado. (No H2, que não tem DDL
		// transacional, o Flyway registra a V6 como falha; no PostgreSQL a migração inteira é desfeita.)
		try (Connection conexao = banco.getConnection();
				Statement comando = conexao.createStatement();
				ResultSet resultado = comando.executeQuery("SELECT COUNT(*) FROM information_schema.columns "
						+ "WHERE table_name = 'quarto' AND column_name = 'tipo'")) {
			resultado.next();
			assertThat(resultado.getInt(1)).isZero();
		}
	}

	@Test
	void migraUmBancoSemQuartos() throws SQLException {
		migrarAte("6");

		assertThat(tipoPorNumero()).isEmpty();
	}

	@Test
	void bancoRecusaTipoDesconhecido() {
		migrarAte("6");

		assertThatThrownBy(() -> executar(
				"INSERT INTO quarto (numero, andar, capacidade_maxima, tipo) VALUES ('101', 1, 1, 'SUITE')"))
				.isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getErrorCode()).isEqualTo(VIOLACAO_CHECK));
	}

}
