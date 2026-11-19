package br.pucminas.hospital.dto;

import br.pucminas.hospital.model.FiltroAtendimentos;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class SituacaoReconhecidaValidator implements ConstraintValidator<SituacaoReconhecida, String> {

	@Override
	public boolean isValid(String situacao, ConstraintValidatorContext context) {
		Set<String> reconhecidas = FiltroAtendimentos.situacoesReconhecidas();
		if (situacao == null || reconhecidas.contains(situacao)) {
			return true;
		}
		// O valor recebido não entra no texto: a mensagem é interpretada como modelo pelo validador.
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate("A situação informada não foi reconhecida. Use um destes "
				+ "códigos: " + String.join(", ", reconhecidas) + ".").addConstraintViolation();
		return false;
	}

}
