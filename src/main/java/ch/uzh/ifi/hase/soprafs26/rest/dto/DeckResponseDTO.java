package ch.uzh.ifi.hase.soprafs26.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// this is used to handle the responses from the deck-of-card api calls

// ignore attributes from API response that are not present in our class
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeckResponseDTO {
    
    // the api returns a deck id (with key deck_id)
    @JsonProperty("deck_id")
    private String deck_id;
    // and a list of cards
    private List<CardDTO> cards;

    public void setDeckId(String deck_id) {
        this.deck_id = deck_id;
    }

    public String getDeckId() {
        return deck_id;
    }

    public void setCards(List<CardDTO> cards) {
        this.cards = cards;
    }

    public List<CardDTO> getCards() {
        return cards;
    }
}