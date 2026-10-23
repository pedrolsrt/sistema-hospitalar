package br.pucminas.hospital.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucminas.hospital.exception.RegraNegocioException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProfissionalSaudeTest {

	@Nested
	class ClinicoGeralTest {

		private final ProfissionalSaude clinico = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 11111");

		@ParameterizedTest
		@EnumSource(AreaAtuacao.class)
		void naoAtendeNenhumaAreaEspecifica(AreaAtuacao area) {
			assertThat(clinico.atendeArea(area)).isFalse();
		}

		@Test
		void naoPodeRealizarProcedimentos() {
			assertThat(clinico.podeRealizarProcedimentos()).isFalse();
		}

		@ParameterizedTest
		@EnumSource(AreaAtuacao.class)
		void naoRealizaConsultaEspecializada(AreaAtuacao area) {
			assertThat(clinico.podeRealizarConsultaEspecializada(area)).isFalse();
		}

	}

	@Nested
	class MedicoEspecialistaTest {

		private final MedicoEspecialista cardiologista =
				new MedicoEspecialista("Dr. João Souza", "CRM-MG 22222", AreaAtuacao.CARDIOLOGIA);

		@Test
		void atendeSuaAreaDeAtuacao() {
			assertThat(cardiologista.atendeArea(AreaAtuacao.CARDIOLOGIA)).isTrue();
		}

		@Test
		void naoAtendeOutraArea() {
			assertThat(cardiologista.atendeArea(AreaAtuacao.NEUROLOGIA)).isFalse();
			assertThat(cardiologista.atendeArea(null)).isFalse();
		}

		@Test
		void naoPodeRealizarProcedimentos() {
			assertThat(cardiologista.podeRealizarProcedimentos()).isFalse();
		}

		@Test
		void realizaConsultaEspecializadaSoNaSuaArea() {
			assertThat(cardiologista.podeRealizarConsultaEspecializada(AreaAtuacao.CARDIOLOGIA)).isTrue();
			assertThat(cardiologista.podeRealizarConsultaEspecializada(AreaAtuacao.NEUROLOGIA)).isFalse();
		}

		@Test
		void passaAAtenderNovaAreaAoSerAlterada() {
			cardiologista.setAreaAtuacao(AreaAtuacao.PEDIATRIA);

			assertThat(cardiologista.atendeArea(AreaAtuacao.PEDIATRIA)).isTrue();
			assertThat(cardiologista.atendeArea(AreaAtuacao.CARDIOLOGIA)).isFalse();
		}

		@Test
		void exigeAreaDeAtuacao() {
			assertThatThrownBy(() -> new MedicoEspecialista("Dr. João Souza", "CRM-MG 22222", null))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("A área de atuação do médico especialista é obrigatória.");
		}

		@Test
		void naoPermiteRemoverAreaDeAtuacao() {
			assertThatThrownBy(() -> cardiologista.setAreaAtuacao(null)).isInstanceOf(RegraNegocioException.class);
			assertThat(cardiologista.getAreaAtuacao()).isEqualTo(AreaAtuacao.CARDIOLOGIA);
		}

	}

	@Nested
	class CirurgiaoTest {

		private final Cirurgiao cirurgiao =
				new Cirurgiao("Dra. Carla Mendes", "CRM-MG 33333", AreaAtuacao.CIRURGIA_GERAL);

		@Test
		void atendeSuaAreaCirurgica() {
			assertThat(cirurgiao.atendeArea(AreaAtuacao.CIRURGIA_GERAL)).isTrue();
		}

		@Test
		void naoAtendeOutraArea() {
			assertThat(cirurgiao.atendeArea(AreaAtuacao.ORTOPEDIA)).isFalse();
			assertThat(cirurgiao.atendeArea(null)).isFalse();
		}

		@Test
		void podeRealizarProcedimentos() {
			assertThat(cirurgiao.podeRealizarProcedimentos()).isTrue();
		}

		@ParameterizedTest
		@EnumSource(AreaAtuacao.class)
		void naoRealizaConsultaEspecializadaNemNaSuaArea(AreaAtuacao area) {
			assertThat(cirurgiao.podeRealizarConsultaEspecializada(area)).isFalse();
		}

		@Test
		void exigeAreaCirurgica() {
			assertThatThrownBy(() -> new Cirurgiao("Dra. Carla Mendes", "CRM-MG 33333", null))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("A área cirúrgica do cirurgião é obrigatória.");
		}

		@Test
		void naoPermiteRemoverAreaCirurgica() {
			assertThatThrownBy(() -> cirurgiao.setAreaCirurgica(null)).isInstanceOf(RegraNegocioException.class);
			assertThat(cirurgiao.getAreaCirurgica()).isEqualTo(AreaAtuacao.CIRURGIA_GERAL);
		}

	}

	@ParameterizedTest
	@EnumSource(AreaAtuacao.class)
	void todaAreaTemDescricaoParaExibicao(AreaAtuacao area) {
		assertThat(area.getDescricao()).isNotBlank();
	}

	@ParameterizedTest
	@EnumSource(CategoriaProfissional.class)
	void fabricaCriaProfissionalDaCategoria(CategoriaProfissional categoria) {
		ProfissionalSaude profissional = categoria.criar("Dra. Ana Lima", "CRM-MG 11111", AreaAtuacao.PEDIATRIA);

		assertThat(profissional.getCategoria()).isEqualTo(categoria);
		assertThat(profissional.getNome()).isEqualTo("Dra. Ana Lima");
		assertThat(profissional.getRegistroProfissional()).isEqualTo("CRM-MG 11111");
		assertThat(profissional.getArea()).isEqualTo(categoria.exigeArea() ? AreaAtuacao.PEDIATRIA : null);
	}

	@Test
	void fabricaRepassaAFaltaDeAreaParaAValidacaoDoDominio() {
		assertThatThrownBy(() -> CategoriaProfissional.MEDICO_ESPECIALISTA.criar("Dr. João", "CRM-MG 2", null))
				.isInstanceOf(RegraNegocioException.class);
		assertThatThrownBy(() -> CategoriaProfissional.CIRURGIAO.criar("Dra. Carla", "CRM-MG 3", null))
				.isInstanceOf(RegraNegocioException.class);
	}

}
