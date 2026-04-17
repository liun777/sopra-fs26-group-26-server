package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.entity.Card;

import java.util.ArrayList;
import java.util.List;

// response for /abilities/peek and /abilities/spy
public class PeekResultDTO {

    private List<Card> revealedCards = new ArrayList<>();

    public List<Card> getRevealedCards() {
        return revealedCards;
    }

    public void setRevealedCards(List<Card> revealedCards) {
        this.revealedCards = revealedCards != null ? revealedCards : new ArrayList<>();
    }
}
