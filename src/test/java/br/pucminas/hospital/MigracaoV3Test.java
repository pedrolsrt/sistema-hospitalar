package br.pucminas.hospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Aplica a V3 sobre um banco com dados da Sprint 1. Cada teste usa um H2 próprio, separado do banco do
 * contexto Spring, para poder parar a migração na V2.
 */
class MigracaoV3Test {

	private String url;

	@BeforeEach
	void criarBanco() {
		url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
	}

	private void migrarAte(String versao) {
		Flyway.configure().dataSource(url, "sa", "").target(versao).load().migrate();
	}

	private void executar(String sql) throws SQLException {
		try (Connection conexao = DriverManager.getConnection(url, "sa", "");
				Statement comando = conexao.createStatement()) {
			comando.executeUpdate(sql);
		}
	}

	@Test
	void profissionaisExistentesPassamASerClinicosGerais() throws SQLException {
		migrarAte("2");
		executar("INSERT INTO profissional_saude (nome, registro_profissional, especialidade) "
				+ "VALUES ('Dr. João Souza', 'CRM-MG 12345', 'Cardiologia')");

		migrarAte("3");

		try (Connection conexao = DriverManager.getConnection(url, "sa", "");
				Statement comando = conexao.createStatement();
				ResultSet resultado = comando.executeQuery(
						"SELECT tipo, area_atuacao, area_cirurgica FROM profissional_saude")) {
			assertThat(resultado.next()).isTrue();
			assertThat(resultado.getString("tipo")).isEqualTo("CLINICO_GERAL");
			assertThat(resultado.getString("area_atuacao")).isNull();
			assertThat(resultado.getString("area_cirurgica")).isNull();
			assertThat(resultado.next()).isFalse();
		}
	}

	@Test
	void bancoRecusaEspecialistaSemArea() {
		migrarAte("3");

		assertThatThrownBy(() -> executar("INSERT INTO profissional_saude (nome, registro_profissional, tipo) "
				+ "VALUES ('Dr. João Souza', 'CRM-MG 12345', 'MEDICO_ESPECIALISTA')"))
				.isInstanceOf(SQLException.class);
	}

	@Test
	void bancoRecusaAreaCirurgicaForaDoCirurgiao() {
		migrarAte("3");

		assertThatThrownBy(() -> executar("INSERT INTO profissional_saude "
				+ "(nome, registro_profissional, tipo, area_cirurgica) "
				+ "VALUES ('Dra. Ana Lima', 'CRM-MG 11111', 'CLINICO_GERAL', 'CIRURGIA_GERAL')"))
				.isInstanceOf(SQLException.class);
	}

}
