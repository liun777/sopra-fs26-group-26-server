package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;

// representation for a response to the client, used by GameStateBroadcastDTO
// card visibility based on what user will see this object
public class PlayerBoardViewDTO {

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
