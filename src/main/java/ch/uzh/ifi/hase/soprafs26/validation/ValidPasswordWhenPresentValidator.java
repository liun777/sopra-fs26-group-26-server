package ch.uzh.ifi.hase.soprafs26.validation;

import ch.uzh.ifi.hase.soprafs26.util.AuthValidationRules;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPasswordWhenPresentValidator implements ConstraintValidator<ValidPasswordWhenPresent, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        int len = value.length();
        if (len < AuthValidationRules.PASSWORD_MIN_LENGTH || len > AuthValidationRules.PASSWORD_MAX_LENGTH) {
            return false;
        }
        return AuthValidationRules.CREDENTIAL_FORMAT_PATTERN.matcher(value).matches();
    }
}
