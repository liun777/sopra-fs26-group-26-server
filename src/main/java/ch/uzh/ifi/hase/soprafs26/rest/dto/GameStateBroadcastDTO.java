package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;

// representation for a response to the client
// filled based on filtering: draw pile represented with a number of cards left; hands of cards are filtered per player
// each player will receive their adjusted instance of this 
public class GameStateBroadcastDTO {

    private String gameId;
    private GameStatus status;
    private int drawPileCount;
    private Long currentTurnUserId;
    private DiscardTopDTO discardPileTop;
    private CardViewDTO drawnCard;
    private boolean caboCalled;
    private long turnSeconds;
    private long initialPeekSeconds;
    private long abilityRevealSeconds;
    private long rematchDecisionSeconds;
    private long afkTimeoutSeconds;
    private GameMoveEventDTO lastMoveEvent;
    private List<PlayerHandViewDTO> players = new ArrayList<>();

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
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

    public CardViewDTO getDrawnCard() {
        return drawnCard;
    }

    public void setDrawnCard(CardViewDTO drawnCard) {
        this.drawnCard = drawnCard;
    }

    public boolean isCaboCalled() {
        return caboCalled;
    }

    public void setCaboCalled(boolean caboCalled) {
        this.caboCalled = caboCalled;
    }

    public long getTurnSeconds() {
        return turnSeconds;
    }

    public void setTurnSeconds(long turnSeconds) {
        this.turnSeconds = turnSeconds;
    }

    public long getInitialPeekSeconds() {
        return initialPeekSeconds;
    }

    public void setInitialPeekSeconds(long initialPeekSeconds) {
        this.initialPeekSeconds = initialPeekSeconds;
    }

    public long getAbilityRevealSeconds() {
        return abilityRevealSeconds;
    }

    public void setAbilityRevealSeconds(long abilityRevealSeconds) {
        this.abilityRevealSeconds = abilityRevealSeconds;
    }

    public long getRematchDecisionSeconds() {
        return rematchDecisionSeconds;
    }

    public void setRematchDecisionSeconds(long rematchDecisionSeconds) {
        this.rematchDecisionSeconds = rematchDecisionSeconds;
    }

    public long getAfkTimeoutSeconds() {
        return afkTimeoutSeconds;
    }

    public void setAfkTimeoutSeconds(long afkTimeoutSeconds) {
        this.afkTimeoutSeconds = afkTimeoutSeconds;
    }

    public GameMoveEventDTO getLastMoveEvent() {
        return lastMoveEvent;
    }

    public void setLastMoveEvent(GameMoveEventDTO lastMoveEvent) {
        this.lastMoveEvent = lastMoveEvent;
    }

    public List<PlayerHandViewDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerHandViewDTO> players) {
        this.players = players;
    }
}
