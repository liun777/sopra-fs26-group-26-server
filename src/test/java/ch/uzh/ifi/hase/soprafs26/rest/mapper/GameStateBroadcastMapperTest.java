package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateBroadcastDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerBoardViewDTO;
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
        assertNull(findBoard(for1, 1L).getCards().get(0).getValue());
        // other player's card
        assertNull(findBoard(for1, 2L).getCards().get(0).getValue());

        GameStateBroadcastDTO for2 = mapper.toBroadcastForViewer(game, 2L);
        // other player's card
        assertNull(findBoard(for2, 1L).getCards().get(0).getValue());
        // own card - but visibility is false
        assertNull(findBoard(for2, 2L).getCards().get(0).getValue());
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
        assertEquals(1, findBoard(for1, 1L).getCards().get(0).getValue().intValue());
        assertEquals("C1", findBoard(for1, 1L).getCards().get(0).getCode());

        GameStateBroadcastDTO for2 = mapper.toBroadcastForViewer(game, 2L);
        // as 2nd player, access cards from 1st player (no data displayed)
        assertNull(findBoard(for2, 1L).getCards().get(0).getValue());
        assertNull(findBoard(for2, 1L).getCards().get(0).getCode());
    }

    // helper to get the PlayerBoardViewDTO instance from GameStateBroadcastDTO instance based on userId match
    private static PlayerBoardViewDTO findBoard(GameStateBroadcastDTO dto, long userId) {
        return dto.getPlayers().stream()
                .filter(p -> p.getUserId() == userId)
                .findFirst()
                .orElseThrow();
    }
}
