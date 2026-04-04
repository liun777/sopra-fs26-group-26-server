package ch.uzh.ifi.hase.soprafs26.entity;

// this is how we define our card entities
public class Card {

    // it has a string code that specifies the value (0-13) and the suits (hearts, diamonds, clubs, spades)
    private String code = "";
    // the value of the card stored as integer
    private int value = 0;
    // whether the card is visible (face-up) or not (face-down)
    private boolean visibility = false;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }
}