package br.pucminas.hospital.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Email;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * E-mail válido e com ponto no domínio. O {@link Email} sozinho aceita domínios sem ponto, como "maria@hospital",
 * que não servem para contato fora de uma rede interna. Nulo é aceito: o e-mail é opcional.
 */
@Email(regexp = "[^@]+@[^@.]+(\\.[^@.]+)+")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailValido {

	String message() default "O e-mail informado não é válido.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
