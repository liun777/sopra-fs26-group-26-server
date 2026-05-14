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

    public static class UsernameRulesDTO {
        private int minLength;
        private int maxLength;
        private String pattern;
        private String allowedCharactersPattern;
        private String hint;

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getAllowedCharactersPattern() {
            return allowedCharactersPattern;
        }

        public void setAllowedCharactersPattern(String allowedCharactersPattern) {
            this.allowedCharactersPattern = allowedCharactersPattern;
        }

        public String getHint() {
            return hint;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }
    }

    public static class PasswordRulesDTO {
        private int minLength;
        private int maxLength;
        private String pattern;
        private String allowedCharactersPattern;
        private String hint;
        private boolean requiresUppercase;
        private boolean requiresSpecialSymbol;
        private boolean asciiOnly;

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getAllowedCharactersPattern() {
            return allowedCharactersPattern;
        }

        public void setAllowedCharactersPattern(String allowedCharactersPattern) {
            this.allowedCharactersPattern = allowedCharactersPattern;
        }

        public String getHint() {
            return hint;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }

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
