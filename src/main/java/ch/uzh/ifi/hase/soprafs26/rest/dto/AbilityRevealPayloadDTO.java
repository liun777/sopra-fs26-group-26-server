package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;


// websocket payload for /user/queue/ability-reveal (7/8 and 9/10 special peek)
// matches client's AbilityRevealPayload in app/game/page.tsx
public class AbilityRevealPayloadDTO {

    private String gameId;
    private String abilityType;
    private List<RevealedCardItemDTO> revealedCards;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getAbilityType() {
        return abilityType;
    }

    public void setAbilityType(String abilityType) {
        this.abilityType = abilityType;
    }

    public List<RevealedCardItemDTO> getRevealedCards() {
        return revealedCards;
    }

    public void setRevealedCards(List<RevealedCardItemDTO> revealedCards) {
        this.revealedCards = revealedCards;
    }

    public static class RevealedCardItemDTO {
        private Integer value;
        private String code;

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
