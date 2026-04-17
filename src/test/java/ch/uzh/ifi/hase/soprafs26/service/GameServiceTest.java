package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PeekSelectionDTO;
import ch.uzh.ifi.hase.soprafs26.util.PeekType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;


public class GameServiceTest {

    private static final String GAME_ID = "id1";

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeckOfCardsAPIService deckOfCardsAPIService;

    @Mock
    private GameEventPublisher gameEventPublisher;

    @InjectMocks
    private GameService gameService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void verify_wrongPlayer_throwsForbidden() {
        User wrongUser = new User();
        wrongUser.setId(2L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(wrongUser);

        Game game = new Game();
        game.setCurrentPlayerId(1L);
        Mockito.when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.verifyMoveCallerIsCurrentPlayer(GAME_ID, "token"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Not your turn", ex.getReason());
    }

    @Test
    void applyInitialPeek_validTwoIndices_revealsOnlyThoseMarksPeekDoneAndPublishes() {
        User player = new User();
        player.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(player);

        List<Card> hand1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            c.setVisibility(false);
            c.setValue(i);
            hand1.add(c);
        }

        List<Card> hand2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            c.setVisibility(false);
            c.setValue(i);
            hand2.add(c);
        }

        Map<Long, List<Card>> hands = new HashMap<>();
        hands.put(1L, hand1);
        hands.put(2L, hand2);

        Game game = new Game();
        game.setId(GAME_ID);
        game.setStatus(GameStatus.INITIAL_PEEK);
        game.setPlayerHands(hands);
        game.setOrderedPlayerIds(List.of(1L, 2L));

        // Optional.of() used because gameRepository.findById() returns Optional<Game>
        Mockito.when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        // when we call gameRepository.save() and pass it an instance of type Game
        // return the game instance that was passed as the argument
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        PeekSelectionDTO body = new PeekSelectionDTO();
        body.setPeekType(PeekType.INITIAL);
        body.setIndices(List.of(0, 2));
        gameService.applyPeek(GAME_ID, "token", body);

        assertTrue(hand1.get(0).getVisibility());
        assertFalse(hand1.get(1).getVisibility());
        assertTrue(hand1.get(2).getVisibility());
        assertFalse(hand1.get(3).getVisibility());
        assertTrue(game.getInitialPeekDoneByUserId().get(1L));
        Mockito.verify(gameEventPublisher).publishFilteredState(game);
    }

    @Test
    void duplicateInitialPeek_throwsForbidden() {
        User player = new User();
        player.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(player);

        List<Card> hand1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            c.setVisibility(false);
            c.setValue(i);
            hand1.add(c);
        }
        Map<Long, List<Card>> hands = new HashMap<>();
        hands.put(1L, hand1);

        Game game = new Game();
        game.setId(GAME_ID);
        game.setStatus(GameStatus.INITIAL_PEEK);
        game.setPlayerHands(hands);

        // Optional.of() used because gameRepository.findById() returns Optional<Game>
        Mockito.when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        // when we call gameRepository.save() and pass it an instance of type Game
        // return the game instance that was passed as the argument
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        PeekSelectionDTO first = new PeekSelectionDTO();
        first.setPeekType(PeekType.INITIAL);
        first.setIndices(List.of(0, 1));
        gameService.applyPeek(GAME_ID, "token", first);

        assertTrue(Boolean.TRUE.equals(game.getInitialPeekDoneByUserId().get(1L)));

        PeekSelectionDTO second = new PeekSelectionDTO();
        second.setPeekType(PeekType.INITIAL);
        second.setIndices(List.of(2, 3));

        // assert an exception is thrown on the second "initial peek" attempt
        // save the exception instance for further checks
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.applyPeek(GAME_ID, "token", second));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Initial peek already used", ex.getReason());
        // verify gameEventPublisher.publishFilteredState was called once during first successful peek only
        Mockito.verify(gameEventPublisher, Mockito.times(1)).publishFilteredState(game);
    }

    // placeholder testing 1/3
    @Test
    void startGame_validPlayers_returnsSavedGameWithId() {
        List<CardDTO> deck = new ArrayList<>();
        for (int i = 0; i < 52; i++) {
            CardDTO dto = new CardDTO();
            dto.setCode("AS");
            deck.add(dto);
        }
        Mockito.when(deckOfCardsAPIService.createNewDeckId()).thenReturn("test-deck-id");
        // doNothing because method is void
        Mockito.doNothing().when(deckOfCardsAPIService).shuffleDeck(Mockito.anyString());
        Mockito.when(deckOfCardsAPIService.drawFromDeck(Mockito.eq("test-deck-id"), Mockito.eq(52))).thenReturn(deck);
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId("game-1");
            return g;
        });
        Mockito.doNothing().when(gameRepository).flush();

        Game result = gameService.startGame(List.of(1L, 2L));

        assertNotNull(result);
        assertEquals("game-1", result.getId());
        assertEquals(2, result.getOrderedPlayerIds().size());
        Mockito.verify(gameEventPublisher, Mockito.times(1)).publishFilteredState(result);
    }
    // placeholder testing 2/3  
    @Test
    void startGame_whenDeckApiFails_usesFallbackDeckAndStillStarts() {
        Mockito.when(deckOfCardsAPIService.createNewDeckId()).thenThrow(new RuntimeException("Deck API down"));
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId("game-fallback");
            return g;
        });
        Mockito.doNothing().when(gameRepository).flush();

        Game result = gameService.startGame(List.of(1L, 2L));

        assertNotNull(result);
        assertEquals("game-fallback", result.getId());
        assertEquals(2, result.getPlayerHands().size());
        assertEquals(4, result.getPlayerHands().get(1L).size());
        assertEquals(4, result.getPlayerHands().get(2L).size());
        assertEquals(43, result.getDrawPile().size());
        assertEquals(1, result.getDiscardPile().size());
        Mockito.verify(gameEventPublisher, Mockito.times(1)).publishFilteredState(result);
    }
    // placeholder testing 3/3
    @Test
    void startGame_withTooFewPlayers_throwsConflict() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gameService.startGame(List.of(1L))
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Lobby requires 2 to 4 players", ex.getReason());
    }

    // this tests that no ability is triggered if the card should not trigger one and that the 
    // turn is immediately passed to the next player
    @Test
    void testCardAbility_cardWithoutAbility_doesNothing() {
        // create a game which is in an active round and a card without ability is drawn from the draw pile
        Game game = new Game();
        game.setId("testGameId");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        List<Long> orderedPlayerIds = List.of(1L, 2L);
        game.setDiscardPile(new ArrayList<>());
        game.setOrderedPlayerIds(orderedPlayerIds);
        game.setCurrentPlayerId(1L);
        Card cardWithoutAbility = new Card();
        cardWithoutAbility.setValue(5);
        game.setDrawnCard(cardWithoutAbility);
        game.setDrawnFromDeck(true);

        Mockito.when(gameRepository.findById("testGameId")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class))).thenReturn(game);

        // call the method to apply the card ability
        gameService.moveCardToDiscardPile("testGameId");

        // make sure the outcome is as we expect for a card without ability
        assertNull(game.getDrawnCard());
        assertTrue(game.getDiscardPile().contains(cardWithoutAbility));
        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());

        // make sure game was saved and state was published
        Mockito.verify(gameRepository, Mockito.times(2)).save(game);
        Mockito.verify(gameEventPublisher, Mockito.times(2)).publishFilteredState(game);
    }

    // this tests that the correct ability is triggered if the card has an ability and that the 
    // turn is not advanced
    @Test
    void testCardAbility_peekCard_setsNewStatus() {
        Game game = new Game();
        game.setId("testGameId");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setDiscardPile(new ArrayList<>());
        List<Long> orderedPlayerIds = List.of(1L, 2L, 3L);
        game.setOrderedPlayerIds(orderedPlayerIds);
        game.setCurrentPlayerId(1L);
        Card peekCard = new Card();
        peekCard.setValue(10);
        game.setDrawnCard(peekCard);
        game.setDrawnFromDeck(true);

        Mockito.when(gameRepository.findById("testGameId")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class))).thenReturn(game);

        gameService.moveCardToDiscardPile("testGameId");

        assertNull(game.getDrawnCard());
        assertTrue(game.getDiscardPile().contains(peekCard));
        assertEquals(GameStatus.ABILITY_PEEK_OPPONENT, game.getStatus());
        assertEquals(1L, game.getCurrentPlayerId());

        Mockito.verify(gameRepository, Mockito.times(1)).save(game);
        Mockito.verify(gameEventPublisher, Mockito.times(1)).publishFilteredState(game);
    }

    // this tests that the that no ability is triggered if the card does not come from the draw 
    // pile and that the turn is advanced to the next player
    @Test
    void testCardAility_swapCardFromDiscardPile_noNewStatus() {
        // set the game up
        Game game = new Game();
        game.setId("testGameId");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setDiscardPile(new ArrayList<>());
        List<Long> orderedPlayerIds = List.of(1L, 2L, 3L);
        game.setOrderedPlayerIds(orderedPlayerIds);
        game.setCurrentPlayerId(1L);
        Card swapCard = new Card();
        swapCard.setValue(11);
        Card topDiscard = new Card();
        topDiscard.setValue(5);
        game.setDrawnCard(topDiscard);
        game.setPlayerHands(Map.of(
                1L, new ArrayList<>(List.of(swapCard, new Card(), new Card(), new Card())),
                2L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card())),
                3L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card()))
        ));
        game.setDrawnFromDeck(false);
        game.setDiscardPile(new ArrayList<>());

        // set up the user
        User user = new User();
        user.setId(1L);
        user.setToken("testToken");

        Mockito.when(gameRepository.findById("testGameId")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class))).thenReturn(game);
        Mockito.when(userRepository.findByToken("testToken")).thenReturn(user);
        
        // apply the move
        gameService.moveSwapDrawnCard("testGameId", "testToken", 0);

        assertNull(game.getDrawnCard());
        assertTrue(game.getDiscardPile().contains(swapCard));
        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());

        Mockito.verify(gameRepository, Mockito.times(2)).save(game);
        Mockito.verify(gameEventPublisher, Mockito.times(2)).publishFilteredState(game);
    }

    @Test
    void applySpecialPeek_selfPeek_revealsThenClearsAndAdvancesTurn() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        List<Card> hand1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            c.setVisibility(false);
            c.setValue(i);
            hand1.add(c);
        }

        List<Card> hand2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            c.setVisibility(false);
            c.setValue(i);
            hand2.add(c);
        }

        Map<Long, List<Card>> hands = new HashMap<>();
        hands.put(1L, hand1);
        hands.put(2L, hand2);

        Game game = new Game();
        game.setId("g-peek-self");
        game.setStatus(GameStatus.ABILITY_PEEK_SELF);
        game.setPlayerHands(hands);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);

        Mockito.when(gameRepository.findById("g-peek-self")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        PeekSelectionDTO body = new PeekSelectionDTO();
        body.setPeekType(PeekType.SPECIAL);
        body.setIndices(List.of(1));

        gameService.applyPeek("g-peek-self", "token", body);

        // game state advanced immediately and cleared the peeked card
        // but we have broadcast it to publishAbilityPeekReveal (see verify below)
        for (Card c : hand1) {
            assertFalse(c.getVisibility());
        }
        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());
        Mockito.verify(gameEventPublisher, Mockito.times(3)).publishFilteredState(game);
        Mockito.verify(gameEventPublisher).publishAbilityPeekReveal(
                Mockito.eq(1L), Mockito.eq("g-peek-self"), Mockito.eq(GameStatus.ABILITY_PEEK_SELF),
                Mockito.argThat(card -> card.getValue() == 1));
    }

    @Test
    void applySpecialPeek_opponentPeek_revealsThenClearsAndAdvancesTurn() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        List<Card> hand1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            hand1.add(c);
        }

        List<Card> hand2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            c.setVisibility(false);
            c.setValue(i);
            hand2.add(c);
        }

        Map<Long, List<Card>> hands = new HashMap<>();
        hands.put(1L, hand1);
        hands.put(2L, hand2);

        Game game = new Game();
        game.setId("g-spy");
        game.setStatus(GameStatus.ABILITY_PEEK_OPPONENT);
        game.setPlayerHands(hands);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);

        Mockito.when(gameRepository.findById("g-spy")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        PeekSelectionDTO body = new PeekSelectionDTO();
        body.setPeekType(PeekType.SPECIAL);
        body.setHandUserId(2L);
        body.setIndices(List.of(2));

        gameService.applyPeek("g-spy", "token", body);

        // game state advanced immediately and cleared the peeked card
        // but we have broadcast it to publishAbilityPeekReveal (see verify below)
        for (Card c : hand2) {
            assertFalse(c.getVisibility());
        }
        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());
        Mockito.verify(gameEventPublisher, Mockito.times(3)).publishFilteredState(game);
        Mockito.verify(gameEventPublisher).publishAbilityPeekReveal(
                Mockito.eq(1L), Mockito.eq("g-spy"), Mockito.eq(GameStatus.ABILITY_PEEK_OPPONENT),
                Mockito.argThat(card -> card.getValue() == 2));
    }

    @Test
    void applySpecialPeek_whenNotInAbilityPhase_throwsConflict() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        List<Card> hand1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card();
            hand1.add(c);
        }

        Map<Long, List<Card>> hands = new HashMap<>();
        hands.put(1L, hand1);

        Game game = new Game();
        game.setId("g-peek-conflict");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setPlayerHands(hands);

        Mockito.when(gameRepository.findById("g-peek-conflict")).thenReturn(Optional.of(game));

        PeekSelectionDTO body = new PeekSelectionDTO();
        body.setPeekType(PeekType.SPECIAL);
        body.setIndices(List.of(0));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.applyPeek("g-peek-conflict", "token", body));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        Mockito.verify(gameEventPublisher, Mockito.never()).publishFilteredState(any());
        Mockito.verify(gameEventPublisher, Mockito.never()).publishAbilityPeekReveal(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
