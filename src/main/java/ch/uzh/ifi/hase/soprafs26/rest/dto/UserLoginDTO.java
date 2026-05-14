package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.util.AuthValidationRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserLoginDTO {
    @NotBlank(message = "Username is required")
    @Size(
            min = AuthValidationRules.USERNAME_MIN_LENGTH,
            max = AuthValidationRules.USERNAME_MAX_LENGTH,
            message = "Username must be between 1 and 16 characters long"
    )
    @Pattern(
            regexp = AuthValidationRules.USERNAME_REGEX,
            message = "Username can only contain ASCII letters and numbers"
    )
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
