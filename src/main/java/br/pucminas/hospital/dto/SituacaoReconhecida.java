package br.pucminas.hospital.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * O código de situação, quando informado, existe em alguma modalidade de atendimento. A mensagem de erro
 * lista os códigos válidos.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SituacaoReconhecidaValidator.class)
public @interface SituacaoReconhecida {

	String message() default "A situação informada não foi reconhecida.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
