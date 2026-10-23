package br.pucminas.hospital;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Aplica a V4 sobre um banco com consultas e internações da Sprint 1. Cada teste usa um H2 próprio,
 * separado do banco do contexto Spring, para poder parar a migração na V3.
 */
class MigracaoV4Test {

	// Uma única conexão para o Flyway e para o teste; o motivo está em MigracaoV3Test.
	private SingleConnectionDataSource banco;

	@BeforeEach
	void criarBancoComDadosDaV3() throws SQLException {
		String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
		banco = new SingleConnectionDataSource(url, "sa", "", true);
		migrarAte("3");
		executar("INSERT INTO paciente (id, nome, cpf, data_nascimento) VALUES (10, 'Maria Silva', '52998224725', DATE '1990-05-10')",
				"INSERT INTO paciente (id, nome, cpf, data_nascimento) VALUES (20, 'José Lima', '11144477735', DATE '1985-02-01')",
				"INSERT INTO profissional_saude (id, nome, registro_profissional, tipo) "
						+ "VALUES (30, 'Dr. João Souza', 'CRM-MG 12345', 'CLINICO_GERAL')",
				"INSERT INTO quarto (id, numero, andar, capacidade_maxima, ocupacao_atual) VALUES (40, '201', 2, 2, 1)",
				// Ids de consulta e de internação colidem de propósito: as duas tabelas tinham sequências próprias.
				"INSERT INTO consulta (id, paciente_id, profissional_responsavel_id, observacoes, data, horario, motivo, status) "
						+ "VALUES (1, 10, 30, 'Retorno em 30 dias', DATE '2030-01-15', TIME '14:00:00', 'Dor no peito', 'REALIZADA')",
				"INSERT INTO consulta (id, paciente_id, profissional_responsavel_id, data, horario, motivo, status) "
						+ "VALUES (2, 20, 30, DATE '2030-01-16', TIME '09:00:00', 'Check-up', 'AGENDADA')",
				"INSERT INTO consulta (id, paciente_id, profissional_responsavel_id, data, horario, motivo, status) "
						+ "VALUES (3, 10, 30, DATE '2030-01-17', TIME '10:30:00', 'Febre', 'CANCELADA')",
				"INSERT INTO internacao (id, paciente_id, profissional_responsavel_id, observacoes, quarto_id, "
						+ "data_entrada, data_prevista_alta) "
						+ "VALUES (1, 20, 30, 'Observação pós-cirúrgica', 40, DATE '2030-02-01', DATE '2030-02-05')");
		migrarAte("4");
	}

	@AfterEach
	void fecharBanco() {
		banco.destroy();
	}

	private void migrarAte(String versao) {
		Flyway.configure().dataSource(banco).target(versao).load().migrate();
	}

	private void executar(String... comandos) throws SQLException {
		try (Connection conexao = banco.getConnection();
				Statement comando = conexao.createStatement()) {
			for (String sql : comandos) {
				comando.executeUpdate(sql);
			}
		}
	}

	private List<Map<String, Object>> consultar(String sql) throws SQLException {
		List<Map<String, Object>> linhas = new ArrayList<>();
		try (Connection conexao = banco.getConnection();
				Statement comando = conexao.createStatement();
				ResultSet resultado = comando.executeQuery(sql)) {
			int colunas = resultado.getMetaData().getColumnCount();
			while (resultado.next()) {
				Map<String, Object> linha = new HashMap<>();
				for (int i = 1; i <= colunas; i++) {
					linha.put(resultado.getMetaData().getColumnLabel(i).toLowerCase(), resultado.getObject(i));
				}
				linhas.add(linha);
			}
		}
		return linhas;
	}

	private Map<String, Object> consultaComMotivo(String motivo) throws SQLException {
		List<Map<String, Object>> linhas = consultar("SELECT a.paciente_id, a.observacoes, c.situacao, c.data, "
				+ "(SELECT COUNT(*) FROM consulta_rotina r WHERE r.id = c.id) AS rotina "
				+ "FROM consulta c JOIN atendimento a ON a.id = c.id WHERE c.motivo = '" + motivo + "'");
		assertThat(linhas).hasSize(1);
		return linhas.get(0);
	}

	@Test
	void cadaConsultaEInternacaoGanhaUmAtendimentoProprio() throws SQLException {
		assertThat(consultar("SELECT id FROM atendimento")).hasSize(4);
		assertThat(consultar("SELECT id FROM consulta")).hasSize(3);
		assertThat(consultar("SELECT id FROM internacao")).hasSize(1);
		assertThat(consultar("SELECT c.id FROM consulta c JOIN internacao i ON i.id = c.id")).isEmpty();
	}

	@Test
	void consultaMantemSeusDadosEPassaASerDeRotina() throws SQLException {
		Map<String, Object> consulta = consultaComMotivo("Dor no peito");

		assertThat(consulta.get("paciente_id")).isEqualTo(10L);
		assertThat(consulta.get("observacoes")).isEqualTo("Retorno em 30 dias");
		assertThat(consulta.get("data").toString()).isEqualTo("2030-01-15");
		assertThat(consulta.get("rotina")).isEqualTo(1L);
		assertThat(consultar("SELECT id FROM consulta_especializada")).isEmpty();
	}

	@Test
	void realizadaViraConcluidaEOsDemaisStatusMantemONome() throws SQLException {
		assertThat(consultaComMotivo("Dor no peito").get("situacao")).isEqualTo("CONCLUIDA");
		assertThat(consultaComMotivo("Check-up").get("situacao")).isEqualTo("AGENDADA");
		assertThat(consultaComMotivo("Febre").get("situacao")).isEqualTo("CANCELADA");
	}

	@Test
	void internacaoMantemSeusDadosEOsDoAtendimento() throws SQLException {
		List<Map<String, Object>> linhas = consultar("SELECT a.paciente_id, a.observacoes, i.quarto_id, i.data_entrada "
				+ "FROM internacao i JOIN atendimento a ON a.id = i.id");

		assertThat(linhas).hasSize(1);
		assertThat(linhas.get(0).get("paciente_id")).isEqualTo(20L);
		assertThat(linhas.get(0).get("observacoes")).isEqualTo("Observação pós-cirúrgica");
		assertThat(linhas.get(0).get("quarto_id")).isEqualTo(40L);
		assertThat(linhas.get(0).get("data_entrada").toString()).isEqualTo("2030-02-01");
	}

	@Test
	void novosAtendimentosRecebemIdsQueNaoColidemComOsConvertidos() throws SQLException {
		executar("INSERT INTO atendimento (paciente_id, profissional_responsavel_id) VALUES (10, 30)");

		assertThat(consultar("SELECT DISTINCT id FROM atendimento")).hasSize(5);
	}

}
