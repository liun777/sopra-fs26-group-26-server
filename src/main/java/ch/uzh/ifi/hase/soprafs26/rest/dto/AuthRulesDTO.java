package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class AuthRulesDTO {
    private UsernameRulesDTO username;
    private PasswordRulesDTO password;

    public UsernameRulesDTO getUsername() {
        return username;
    }

    public void setUsername(UsernameRulesDTO username) {
        this.username = username;
    }

    public PasswordRulesDTO getPassword() {
        return password;
    }

    public void setPassword(PasswordRulesDTO password) {
        this.password = password;
    }

    public static class UsernameRulesDTO extends AuthFieldRulesDTO {
    }

    public static class PasswordRulesDTO extends AuthFieldRulesDTO {
        private boolean requiresUppercase;
        private boolean requiresSpecialSymbol;
        private boolean asciiOnly;

        public boolean isRequiresUppercase() {
            return requiresUppercase;
        }

        public void setRequiresUppercase(boolean requiresUppercase) {
            this.requiresUppercase = requiresUppercase;
        }

        public boolean isRequiresSpecialSymbol() {
            return requiresSpecialSymbol;
        }

        public void setRequiresSpecialSymbol(boolean requiresSpecialSymbol) {
            this.requiresSpecialSymbol = requiresSpecialSymbol;
        }

        public boolean isAsciiOnly() {
            return asciiOnly;
        }

        public void setAsciiOnly(boolean asciiOnly) {
            this.asciiOnly = asciiOnly;
        }
    }
}
