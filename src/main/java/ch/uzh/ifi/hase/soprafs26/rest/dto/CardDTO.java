package ch.uzh.ifi.hase.soprafs26.rest.dto;

// this is how the deck-of-cards api returns one card
public class CardDTO {

    // we only need the code of the card but actually there would be other fields
    private String code;

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}