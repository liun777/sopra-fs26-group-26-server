package ch.uzh.ifi.hase.soprafs26.rest.dto;

// representation for a response to the client
// top discard card - public for everyone
public class DiscardTopDTO {

    private int value;
    private String code;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
