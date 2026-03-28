package ch.uzh.ifi.hase.soprafs26.entity;

public class Card {

    private int value = 0;
    private boolean visibility = false;
    private String ability = "none";

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

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }
}