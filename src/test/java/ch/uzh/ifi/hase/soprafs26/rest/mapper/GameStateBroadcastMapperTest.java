package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardViewDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateBroadcastDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerHandViewDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameStateBroadcastMapperTest {

    private GameStateBroadcastMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GameStateBroadcastMapper();
    }

    @Test
    void twoViewers_drawCountOnly_opponentHandSecretsHidden() {
        Game game = new Game();
        game.setId("g1");

        List<Card> draw = new ArrayList<>();
        Card secret = new Card();
        secret.setValue(1);
        secret.setCode("C1");
        secret.setVisibility(false);
        draw.add(secret);
        game.setDrawPile(draw);

        Map<Long, List<Card>> hands = new HashMap<>();
        Card h1 = new Card();
        h1.setValue(2);
        h1.setCode("C2");
        h1.setVisibility(false);
        hands.put(1L, List.of(h1));
        Card h2 = new Card();
        h2.setValue(3);
        h2.setCode("C3");
        h2.setVisibility(false);
        hands.put(2L, List.of(h2));
        game.setPlayerHands(hands);
        game.setOrderedPlayerIds(List.of(1L, 2L));

        GameStateBroadcastDTO for1 = mapper.toBroadcastForViewer(game, 1L);
        assertEquals(1, for1.getDrawPileCount());
        // own card - but visibility is false
        assertNull(findPlayerHand(for1, 1L).getCards().get(0).getValue());
        // other player's card
        assertNull(findPlayerHand(for1, 2L).getCards().get(0).getValue());

        GameStateBroadcastDTO for2 = mapper.toBroadcastForViewer(game, 2L);
        // other player's card
        assertNull(findPlayerHand(for2, 1L).getCards().get(0).getValue());
        // own card - but visibility is false
        assertNull(findPlayerHand(for2, 2L).getCards().get(0).getValue());
    }

    // #47: player 1 sees values only on their two peeked cards; player 2 never sees player 1's values.
    @Test
    void twoVisibleCardsOnPlayer1Hand_player2SeesNoValues() {
        Game game = new Game();
        game.setId("game-peek");

        Map<Long, List<Card>> hands = new HashMap<>();

        List<Card> hand1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            c.setValue(i);
            c.setCode(i + "-hearts");
            c.setVisibility(i == 0 || i == 2);
            hand1.add(c);
        }
        hands.put(1L, hand1);

        List<Card> hand2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            c.setValue(i);
            c.setCode(i + "-clubs");
            c.setVisibility(false);
            hand2.add(c);
        }
        hands.put(2L, hand2);

        game.setPlayerHands(hands);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        // current player viewing as player 2 must not see player 1's peeked cards during INITIAL_PEEK
        // old mapper leaked when isViewerCurrentPlayer && !isOwner && card.getVisibility()
        game.setCurrentPlayerId(2L);
        game.setStatus(GameStatus.INITIAL_PEEK);

        GameStateBroadcastDTO gameStateForPlayer1 = mapper.toBroadcastForViewer(game, 1L);
        List<CardViewDTO> player1HandPlayer1View = findPlayerHand(gameStateForPlayer1, 1L).getCards();
        assertEquals(0, player1HandPlayer1View.get(0).getValue().intValue());
        assertEquals("0-hearts", player1HandPlayer1View.get(0).getCode());
        assertNull(player1HandPlayer1View.get(1).getValue());
        assertEquals(2, player1HandPlayer1View.get(2).getValue().intValue());
        assertEquals("2-hearts", player1HandPlayer1View.get(2).getCode());
        assertNull(player1HandPlayer1View.get(3).getValue());

        GameStateBroadcastDTO gameStateForPlayer2 = mapper.toBroadcastForViewer(game, 2L);
        List<CardViewDTO> player1HandPlayer2View = findPlayerHand(gameStateForPlayer2, 1L).getCards();
        for (int i = 0; i < 4; i++) {
            assertNull(player1HandPlayer2View.get(i).getValue());
            assertNull(player1HandPlayer2View.get(i).getCode());
        }
    }

    @Test
    void ownerVisibleCard_showsValueAndCode() {
        Game game = new Game();
        game.setId("g2");
        Map<Long, List<Card>> hands = new HashMap<>();
        Card c = new Card();
        c.setValue(1);
        c.setCode("C1");
        c.setVisibility(true);
        hands.put(1L, List.of(c));
        game.setPlayerHands(hands);
        game.setOrderedPlayerIds(List.of(1L, 2L));

        GameStateBroadcastDTO for1 = mapper.toBroadcastForViewer(game, 1L);
        assertEquals(1, findPlayerHand(for1, 1L).getCards().get(0).getValue().intValue());
        assertEquals("C1", findPlayerHand(for1, 1L).getCards().get(0).getCode());

        GameStateBroadcastDTO for2 = mapper.toBroadcastForViewer(game, 2L);
        // as 2nd player, access cards from 1st player (no data displayed)
        assertNull(findPlayerHand(for2, 1L).getCards().get(0).getValue());
        assertNull(findPlayerHand(for2, 1L).getCards().get(0).getCode());
    }

    // helper to get the PlayerHandViewDTO instance from GameStateBroadcastDTO instance based on userId match
    private static PlayerHandViewDTO findPlayerHand(GameStateBroadcastDTO dto, long userId) {
        return dto.getPlayers().stream()
                .filter(p -> p.getUserId() == userId)
                .findFirst()
                .orElseThrow();
    }
}
