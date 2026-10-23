package br.pucminas.hospital.mapper;

import br.pucminas.hospital.dto.EnderecoDto;
import br.pucminas.hospital.dto.PacienteAtualizacaoRequest;
import br.pucminas.hospital.dto.PacienteRequest;
import br.pucminas.hospital.dto.PacienteResponse;
import br.pucminas.hospital.model.Endereco;
import br.pucminas.hospital.model.Paciente;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PacienteMapper {

	// CPF e CEP são gravados só com os dígitos; a requisição pode trazê-los com pontuação.
	public Paciente paraEntidade(PacienteRequest request) {
		Paciente paciente = new Paciente(request.nome().strip(), Textos.somenteDigitos(request.cpf()),
				request.dataNascimento());
		paciente.setTelefone(Textos.vazioComoNulo(request.telefone()));
		paciente.setEmail(Textos.vazioComoNulo(request.email()));
		paciente.setEndereco(paraEndereco(request.endereco()));
		return paciente;
	}

	public void atualizar(Paciente paciente, PacienteAtualizacaoRequest request) {
		paciente.setNome(request.nome().strip());
		paciente.setDataNascimento(request.dataNascimento());
		paciente.setTelefone(Textos.vazioComoNulo(request.telefone()));
		paciente.setEmail(Textos.vazioComoNulo(request.email()));
		paciente.setEndereco(paraEndereco(request.endereco()));
	}

	public PacienteResponse paraResponse(Paciente paciente) {
		return new PacienteResponse(paciente.getId(), paciente.getNome(), paciente.getCpf(),
				paciente.getDataNascimento(), paciente.getTelefone(), paciente.getEmail(),
				paraDto(paciente.getEndereco()), paciente.isAtivo());
	}

	private static Endereco paraEndereco(EnderecoDto dto) {
		if (dto == null) {
			return null;
		}
		return new Endereco(dto.logradouro().strip(), Textos.vazioComoNulo(dto.numero()),
				Textos.vazioComoNulo(dto.complemento()), Textos.vazioComoNulo(dto.bairro()), dto.cidade().strip(),
				dto.uf().toUpperCase(Locale.ROOT), dto.cep() == null ? null : Textos.somenteDigitos(dto.cep()));
	}

	private static EnderecoDto paraDto(Endereco endereco) {
		if (endereco == null) {
			return null;
		}
		return new EnderecoDto(endereco.getLogradouro(), endereco.getNumero(), endereco.getComplemento(),
				endereco.getBairro(), endereco.getCidade(), endereco.getUf(), endereco.getCep());
	}

}
