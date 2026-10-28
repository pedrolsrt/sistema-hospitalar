package br.pucminas.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.pucminas.hospital.dto.AtendimentoEmergenciaResponse;
import br.pucminas.hospital.dto.ConsultaResponse;
import br.pucminas.hospital.dto.HistoricoItemResponse;
import br.pucminas.hospital.dto.HistoricoMedicoResponse;
import br.pucminas.hospital.dto.InternacaoResponse;
import br.pucminas.hospital.dto.ProcedimentoCirurgicoResponse;
import br.pucminas.hospital.exception.RecursoNaoEncontradoException;
import br.pucminas.hospital.mapper.AtendimentoEmergenciaMapper;
import br.pucminas.hospital.mapper.ConsultaMapper;
import br.pucminas.hospital.mapper.HistoricoMedicoMapper;
import br.pucminas.hospital.mapper.InternacaoMapper;
import br.pucminas.hospital.mapper.ProcedimentoCirurgicoMapper;
import br.pucminas.hospital.model.AreaAtuacao;
import br.pucminas.hospital.model.AtendimentoEmergencia;
import br.pucminas.hospital.model.Cirurgiao;
import br.pucminas.hospital.model.ClinicoGeral;
import br.pucminas.hospital.model.ConsultaEspecializada;
import br.pucminas.hospital.model.ConsultaRotina;
import br.pucminas.hospital.model.Internacao;
import br.pucminas.hospital.model.MedicoEspecialista;
import br.pucminas.hospital.model.Paciente;
import br.pucminas.hospital.model.PrioridadeEmergencia;
import br.pucminas.hospital.model.ProcedimentoCirurgico;
import br.pucminas.hospital.model.Quarto;
import br.pucminas.hospital.model.TipoCirurgia;
import br.pucminas.hospital.model.TipoQuarto;
import br.pucminas.hospital.repository.AtendimentoRepository;
import br.pucminas.hospital.repository.PacienteRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoricoMedicoServiceTest {

	private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private AtendimentoRepository atendimentoRepository;

	private HistoricoMedicoService service;

	private Paciente maria;

	@BeforeEach
	void configurar() {
		HistoricoMedicoMapper mapper = new HistoricoMedicoMapper(new ConsultaMapper(), new InternacaoMapper(),
				new AtendimentoEmergenciaMapper(), new ProcedimentoCirurgicoMapper());
		service = new HistoricoMedicoService(pacienteRepository, atendimentoRepository, mapper);
		maria = new Paciente("Maria Silva", "52998224725", LocalDate.of(1990, 5, 10));
	}

	@Test
	void reuneTodasAsModalidadesComOsDadosDeCada() {
		ClinicoGeral clinico = new ClinicoGeral("Dra. Ana Lima", "CRM-MG 1");
		MedicoEspecialista cardiologista = new MedicoEspecialista("Dr. João Souza", "CRM-MG 2", AreaAtuacao.CARDIOLOGIA);
		Cirurgiao cirurgiao = new Cirurgiao("Dra. Carla Mendes", "CRM-MG 3", AreaAtuacao.CIRURGIA_GERAL);
		AtendimentoEmergencia emergencia = new AtendimentoEmergencia(maria, clinico, PrioridadeEmergencia.ALTA,
				DIA.minusDays(1).atTime(22, 0));
		Internacao internacao = new Internacao(maria, clinico, new Quarto("101", 1, TipoQuarto.ENFERMARIA, 4), DIA,
				DIA.plusDays(4));
		ProcedimentoCirurgico cirurgia = new ProcedimentoCirurgico(maria, cirurgiao, "Apendicectomia",
				TipoCirurgia.URGENCIA, DIA.plusDays(1).atTime(8, 0));
		ConsultaRotina rotina = new ConsultaRotina(maria, clinico, DIA.plusDays(2), LocalTime.of(9, 0), "Retorno");
		ConsultaEspecializada especializada = new ConsultaEspecializada(maria, cardiologista, DIA.plusDays(3),
				LocalTime.of(10, 0), "Avaliação", AreaAtuacao.CARDIOLOGIA);
		when(pacienteRepository.findById(1L)).thenReturn(Optional.of(maria));
		when(atendimentoRepository.findHistoricoDoPaciente(1L))
				.thenReturn(List.of(emergencia, internacao, cirurgia, rotina, especializada));

		HistoricoMedicoResponse historico = service.montar(1L);

		assertThat(historico.paciente().nome()).isEqualTo("Maria Silva");
		List<HistoricoItemResponse> itens = historico.atendimentos();
		assertThat(itens).extracting(item -> item.modalidade().codigo()).containsExactly("EMERGENCIA", "INTERNACAO",
				"PROCEDIMENTO_CIRURGICO", "CONSULTA_ROTINA", "CONSULTA_ESPECIALIZADA");
		assertThat(itens).extracting(HistoricoItemResponse::dataHora).containsExactly(
				DIA.minusDays(1).atTime(22, 0), DIA.atStartOfDay(), DIA.plusDays(1).atTime(8, 0),
				DIA.plusDays(2).atTime(9, 0), DIA.plusDays(3).atTime(10, 0));

		assertThat(itens.get(0).detalhes()).isInstanceOfSatisfying(AtendimentoEmergenciaResponse.class,
				e -> assertThat(e.prioridade().codigo()).isEqualTo("ALTA"));
		assertThat(itens.get(1).detalhes()).isInstanceOfSatisfying(InternacaoResponse.class,
				i -> assertThat(i.quarto().numero()).isEqualTo("101"));
		assertThat(itens.get(2).detalhes()).isInstanceOfSatisfying(ProcedimentoCirurgicoResponse.class,
				p -> assertThat(p.descricao()).isEqualTo("Apendicectomia"));
		assertThat(itens.get(3).detalhes()).isInstanceOfSatisfying(ConsultaResponse.class,
				c -> assertThat(c.areaAtuacao()).isNull());
		assertThat(itens.get(4).detalhes()).isInstanceOfSatisfying(ConsultaResponse.class,
				c -> assertThat(c.areaAtuacao().codigo()).isEqualTo("CARDIOLOGIA"));
		assertThat(itens).extracting(item -> item.detalhes().paciente().nome()).containsOnly("Maria Silva");
	}

	@Test
	void historicoVazio() {
		when(pacienteRepository.findById(1L)).thenReturn(Optional.of(maria));
		when(atendimentoRepository.findHistoricoDoPaciente(1L)).thenReturn(List.of());

		assertThat(service.montar(1L).atendimentos()).isEmpty();
	}

	@Test
	void pacienteInexistente() {
		assertThatThrownBy(() -> service.montar(9L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Paciente não encontrado (id 9).");
	}

}
