package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;

// player's hand as seen by some viewer 
// used by GameStateBroadcastDTO, which is filtered per player
public class PlayerHandViewDTO {

    private Long userId;
    private String username;
    private String profileCharacterId;
    private String characterColorId;
    private List<CardViewDTO> cards = new ArrayList<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileCharacterId() {
        return profileCharacterId;
    }

    public void setProfileCharacterId(String profileCharacterId) {
        this.profileCharacterId = profileCharacterId;
    }

    public String getCharacterColorId() {
        return characterColorId;
    }

    public void setCharacterColorId(String characterColorId) {
        this.characterColorId = characterColorId;
    }

    public List<CardViewDTO> getCards() {
        return cards;
    }

    public void setCards(List<CardViewDTO> cards) {
        this.cards = cards;
    }
}
