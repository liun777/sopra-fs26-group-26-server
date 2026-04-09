package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;


// representation for a response to the client
// filled based on filtering: draw pile represented with a number of cards left; hands of cards are filtered per player
// each player will receive their adjusted instance of this 
public class GameStateBroadcastDTO {

    private String gameId;
    private int drawPileCount;
    private Long currentTurnUserId;
    private DiscardTopDTO discardPileTop;
    private List<PlayerHandViewDTO> players = new ArrayList<>();

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public int getDrawPileCount() {
        return drawPileCount;
    }

    public void setDrawPileCount(int drawPileCount) {
        this.drawPileCount = drawPileCount;
    }

    public Long getCurrentTurnUserId() {
        return currentTurnUserId;
    }

    public void setCurrentTurnUserId(Long currentTurnUserId) {
        this.currentTurnUserId = currentTurnUserId;
    }

    public DiscardTopDTO getDiscardPileTop() {
        return discardPileTop;
    }

    public void setDiscardPileTop(DiscardTopDTO discardPileTop) {
        this.discardPileTop = discardPileTop;
    }

    public List<PlayerHandViewDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerHandViewDTO> players) {
        this.players = players;
    }
}
