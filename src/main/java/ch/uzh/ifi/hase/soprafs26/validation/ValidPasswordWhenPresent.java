package ch.uzh.ifi.hase.soprafs26.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Documented
@Constraint(validatedBy = ValidPasswordWhenPresentValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPasswordWhenPresent {

    String message() default "Password must be between 8 and 32 characters long and include 1 uppercase, 1 special symbol, and only ASCII characters (no spaces)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
