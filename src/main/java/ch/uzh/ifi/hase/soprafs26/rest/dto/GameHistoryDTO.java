package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs26.entity.Card;

public class GameHistoryDTO {
    
    private String id;
    private Map<Long, List<Card>> playerHands;
    private List<Long> orderedPlayerIds;
    private Long caboCalledByUserId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

        public Long getCaboCalledByUserId() {
        return caboCalledByUserId;
    }

    public void setCaboCalledByUserId(Long caboCalledByUserId) {
        this.caboCalledByUserId = caboCalledByUserId;
    }

        public List<Long> getOrderedPlayerIds() {
        return orderedPlayerIds;
    }

    public void setOrderedPlayerIds(List<Long> orderedPlayerIds) {
        this.orderedPlayerIds = orderedPlayerIds;
    }

        public  Map<Long, List<Card>> getPlayerHands() {
        return playerHands;
    }

    public void setPlayerHands(Map<Long, List<Card>> playerHands) {
        this.playerHands = playerHands;
    }
}
