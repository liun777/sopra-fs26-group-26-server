package ch.uzh.ifi.hase.soprafs26.rest.dto;


public class AuthFieldRulesDTO {

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
