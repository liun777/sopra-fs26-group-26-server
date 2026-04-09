package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;

// player's hand as seen by some viewer 
// used by GameStateBroadcastDTO, which is filtered per player
public class PlayerHandViewDTO {

    private Long userId;
    private List<CardViewDTO> cards = new ArrayList<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CardViewDTO> getCards() {
        return cards;
    }

    public void setCards(List<CardViewDTO> cards) {
        this.cards = cards;
    }
}
