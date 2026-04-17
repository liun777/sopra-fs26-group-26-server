package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;

@Entity
// create a table with name games in the DB
@Table(name = "games" )
public class Game {
    
    @Id
    // generate a random id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, unique = true)
    private String id;

    // deckofcards API id. null for fallback
    @Column(nullable = true)
    private String deckApiId;

    // the line below is used to save lists and maps to the DB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    // all fields are initialized as empty lists or maps to avoid null pointer errors
    private List<Card> drawPile = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private List<Card> discardPile = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private Map<Long, List<Card>> playerHands = new HashMap<>();

    // to keep a consistent order of players
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private List<Long> orderedPlayerIds = new ArrayList<>();

    // this always stores the card that was drawn last by any player
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = true, unique = false)
    private Card drawnCard;

    // game always starts with peeking
    @Column(nullable = false)
    private GameStatus status = GameStatus.INITIAL_PEEK;

    // Per user: true after successful initial peek (no second initial peek)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private Map<Long, Boolean> initialPeekDoneByUserId = new HashMap<>();

    // tracks whether the current drawn card was drawn from the deck (not from hand)
    // needed to determine if special abilities can be triggered
    @Column(nullable = false)
    private boolean drawnFromDeck = false;

    // tracks whether Cabo has been called
    @Column
    private boolean isCaboCalled = false;

    // tracks who called Cabo
    @Column
    private Long caboCalledByUserId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // do not send to client
    @JsonIgnore
    public String getDeckApiId() {
        return deckApiId;
    }

    public void setDeckApiId(String deckApiId) {
        this.deckApiId = deckApiId;
    }

    public List<Card> getDrawPile() {
        return drawPile;
    }

    public void setDrawPile(List<Card> drawPile) {
        this.drawPile = drawPile;
    }

    public List<Card> getDiscardPile() {
        return discardPile;
    }

    public void setDiscardPile(List<Card> discardPile) {
        this.discardPile = discardPile;
    }

    public  Map<Long, List<Card>> getPlayerHands() {
        return playerHands;
    }

    public void setPlayerHands(Map<Long, List<Card>> playerHands) {
        this.playerHands = playerHands;
    }

    public List<Long> getOrderedPlayerIds() {
        return orderedPlayerIds;
    }

    public void setOrderedPlayerIds(List<Long> orderedPlayerIds) {
        this.orderedPlayerIds = orderedPlayerIds;
    }

    //# 8: Implement a global isMyTurn state that disables all buttons and click listeners on the game board when false.
    @Column(nullable = true)
    private Long currentPlayerId;

    public Long getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(Long currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public Card getDrawnCard() {
        return drawnCard;
    }

    public void setDrawnCard(Card drawnCard) {
        this.drawnCard = drawnCard;
    }

    // peeking at the beginning of the game
    public GameStatus getStatus() {
    return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Map<Long, Boolean> getInitialPeekDoneByUserId() {
        return initialPeekDoneByUserId;
    }

    public void setInitialPeekDoneByUserId(Map<Long, Boolean> initialPeekDoneByUserId) {
        this.initialPeekDoneByUserId =
                initialPeekDoneByUserId != null ? initialPeekDoneByUserId : new HashMap<>();
    }

    public boolean isDrawnFromDeck() {
        return drawnFromDeck;
    }

    public void setDrawnFromDeck(boolean drawnFromDeck) {
        this.drawnFromDeck = drawnFromDeck;
    }

    public boolean isCaboCalled() {
        return isCaboCalled;
    }

    public void setCaboCalled(boolean isCaboCalled) {
        this.isCaboCalled = isCaboCalled;
    }

    public Long getCaboCalledByUserId() {
        return caboCalledByUserId;
    }

    public void setCaboCalledByUserId(Long caboCalledByUserId) {
        this.caboCalledByUserId = caboCalledByUserId;
    }

}
