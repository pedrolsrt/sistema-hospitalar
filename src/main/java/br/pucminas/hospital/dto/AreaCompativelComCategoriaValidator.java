package br.pucminas.hospital.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AreaCompativelComCategoriaValidator
		implements ConstraintValidator<AreaCompativelComCategoria, ProfissionalSaudeRequest> {

	@Override
	public boolean isValid(ProfissionalSaudeRequest request, ConstraintValidatorContext context) {
		// Sem categoria não há o que comparar; a falta dela é apontada pelo @NotNull do campo.
		if (request == null || request.categoria() == null) {
			return true;
		}
		boolean exigeArea = request.categoria().exigeArea();
		boolean informouArea = request.areaAtuacao() != null;
		if (exigeArea == informouArea) {
			return true;
		}
		String descricao = request.categoria().getDescricao();
		String mensagem = exigeArea
				? "Informe a área de atuação para a categoria " + descricao + "."
				: "A categoria " + descricao + " não tem área de atuação; não informe este campo.";
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(mensagem).addPropertyNode("areaAtuacao").addConstraintViolation();
		return false;
	}

}
