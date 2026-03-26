package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Entity
// create a table with name games in the DB
@Table(name = "games" )
public class Game {
    
    @Id
    // generate a random id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, unique = true)
    private String id;

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
}