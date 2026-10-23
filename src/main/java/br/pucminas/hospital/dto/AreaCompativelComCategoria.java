package br.pucminas.hospital.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A área de atuação é informada se, e somente se, a categoria do profissional a exige. O erro é
 * associado ao campo areaAtuacao, para que a resposta 400 aponte o campo certo.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AreaCompativelComCategoriaValidator.class)
public @interface AreaCompativelComCategoria {

	String message() default "A área de atuação não é compatível com a categoria.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
