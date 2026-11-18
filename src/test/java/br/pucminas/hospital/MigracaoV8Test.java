package br.pucminas.hospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Aplica a V8 sobre procedimentos cadastrados antes da existência de salas cirúrgicas. Cada teste usa um H2
 * próprio, separado do banco do contexto Spring, para poder parar a migração na V7.
 */
class MigracaoV8Test {

	private static final int VIOLACAO_CHECK = 23513;
	private static final int VIOLACAO_UNICIDADE = 23505;

	// Uma única conexão para o Flyway e para o teste; o motivo está em MigracaoV3Test.
	private SingleConnectionDataSource banco;

	@BeforeEach
	void criarBancoNaV7() throws SQLException {
		String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
		banco = new SingleConnectionDataSource(url, "sa", "", true);
		migrarAte("7");
		executar("INSERT INTO paciente (id, nome, cpf, data_nascimento) "
						+ "VALUES (10, 'Maria Silva', '52998224725', DATE '1990-05-10')",
				"INSERT INTO profissional_saude (id, nome, registro_profissional, tipo, area_cirurgica) "
						+ "VALUES (30, 'Dra. Carla Mendes', 'CRM-MG 3', 'CIRURGIAO', 'CIRURGIA_GERAL')");
	}

	@AfterEach
	void fecharBanco() {
		banco.destroy();
	}

	private void migrarAte(String versao) {
		Flyway.configure().dataSource(banco).target(versao).load().migrate();
	}

	private void executar(String... comandos) throws SQLException {
		try (Connection conexao = banco.getConnection(); Statement comando = conexao.createStatement()) {
			for (String sql : comandos) {
				comando.executeUpdate(sql);
			}
		}
	}

	private List<String> consultar(String sql) throws SQLException {
		List<String> linhas = new ArrayList<>();
		try (Connection conexao = banco.getConnection();
				Statement comando = conexao.createStatement();
				ResultSet resultado = comando.executeQuery(sql)) {
			while (resultado.next()) {
				linhas.add(resultado.getString(1));
			}
		}
		return linhas;
	}

	private void procedimentoNaV7(long id, String situacao) throws SQLException {
		executar("INSERT INTO atendimento (id, paciente_id, profissional_responsavel_id) VALUES (" + id + ", 10, 30)",
				"INSERT INTO procedimento_cirurgico (id, descricao, tipo_cirurgia, data_hora_prevista, situacao) "
						+ "VALUES (" + id + ", 'Apendicectomia', 'ELETIVA', TIMESTAMP '2030-03-10 08:00:00', "
						+ "'" + situacao + "')");
	}

	@Test
	void procedimentosExistentesRecebemASalaPadraoESessentaMinutos() throws SQLException {
		procedimentoNaV7(1, "AGENDADO");
		procedimentoNaV7(2, "CANCELADO");

		migrarAte("8");

		assertThat(consultar("SELECT identificacao FROM sala_cirurgica")).containsExactly("Sala padrão");
		assertThat(consultar("SELECT s.identificacao || ' ' || p.duracao_prevista_minutos "
				+ "FROM procedimento_cirurgico p JOIN sala_cirurgica s ON s.id = p.sala_cirurgica_id ORDER BY p.id"))
				.containsExactly("Sala padrão 60", "Sala padrão 60");
		assertThat(consultar("SELECT CASE WHEN ativa THEN 'ativa' ELSE 'inativa' END FROM sala_cirurgica"))
				.containsExactly("ativa");
	}

	@Test
	void bancoSemProcedimentosNaoGanhaSala() throws SQLException {
		migrarAte("8");

		assertThat(consultar("SELECT identificacao FROM sala_cirurgica")).isEmpty();
	}

	@Test
	void bancoRecusaDuracaoForaDosLimites() throws SQLException {
		procedimentoNaV7(1, "AGENDADO");
		migrarAte("8");

		assertThatThrownBy(() -> executar("UPDATE procedimento_cirurgico SET duracao_prevista_minutos = 14"))
				.isInstanceOfSatisfying(SQLException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(VIOLACAO_CHECK));
		assertThatThrownBy(() -> executar("UPDATE procedimento_cirurgico SET duracao_prevista_minutos = 721"))
				.isInstanceOfSatisfying(SQLException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(VIOLACAO_CHECK));
		executar("UPDATE procedimento_cirurgico SET duracao_prevista_minutos = 720");
	}

	@Test
	void bancoRecusaIdentificacaoRepetida() throws SQLException {
		migrarAte("8");
		executar("INSERT INTO sala_cirurgica (identificacao) VALUES ('Sala 1')");

		assertThatThrownBy(() -> executar("INSERT INTO sala_cirurgica (identificacao) VALUES ('Sala 1')"))
				.isInstanceOfSatisfying(SQLException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(VIOLACAO_UNICIDADE));
	}

}
