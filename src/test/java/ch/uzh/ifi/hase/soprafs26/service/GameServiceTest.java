package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.config.settings.GameSettingsProperties;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardViewDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateBroadcastDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PeekSelectionDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.GameStateBroadcastMapper;
import ch.uzh.ifi.hase.soprafs26.util.PeekType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private GameSettingsProperties gameSettings;

    @Mock
    private LobbyService lobbyService;

    @InjectMocks
    private GameService gameService;

    private int scheduleCallCount;

    @BeforeEach
    void setup() {
        scheduleCallCount = 0;
        MockitoAnnotations.openMocks(this);
        Mockito.when(gameSettings.getMinPlayers()).thenReturn(2);
        Mockito.when(gameSettings.getMaxPlayers()).thenReturn(4);
        Mockito.when(gameSettings.getStarterCardsPerPlayer()).thenReturn(4);
        Mockito.when(gameSettings.getInitialPeekSeconds()).thenReturn(10L);
        Mockito.when(gameSettings.getTurnSeconds()).thenReturn(30L);
        Mockito.when(gameSettings.getAbilitySeconds()).thenReturn(30L);
        Mockito.when(gameSettings.getPostPeekAutoEndSeconds()).thenReturn(5L);
        Mockito.when(gameSettings.getRematchDecisionSeconds()).thenReturn(60L);
        Mockito.when(lobbyService.isPlayerTimedOutInPlaying(Mockito.anyLong())).thenReturn(false);
        // mock timer without waiting for it
        Mockito.when(scheduler.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenAnswer(invocation -> Mockito.mock(ScheduledFuture.class));
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

    @Test
    void advanceTurnToNextPlayer_timedOutNextPlayer_autoCallsCaboOnThatTurn() {
        Game game = new Game();
        game.setId(GAME_ID);
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setOrderedPlayerIds(new ArrayList<>(List.of(1L, 2L, 3L)));
        game.setCurrentPlayerId(1L);

        Mockito.when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();
        Mockito.when(lobbyService.isPlayerTimedOutInPlaying(2L)).thenReturn(true);

        gameService.advanceTurnToNextPlayer(GAME_ID);

        assertTrue(game.isCaboCalled());
        assertEquals(2L, game.getCaboCalledByUserId());
        assertEquals(3L, game.getCurrentPlayerId());
        verify(lobbyService).isPlayerTimedOutInPlaying(2L);
        verify(gameEventPublisher, Mockito.atLeastOnce()).publishFilteredState(any(Game.class));
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
    void applySpecialPeek_selfPeek_reveals_staysInAbilityPhase() {
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

        assertTrue(hand1.get(1).getVisibility());
        assertFalse(hand1.get(0).getVisibility());
        assertEquals(GameStatus.ABILITY_PEEK_SELF, game.getStatus());
        assertEquals(1L, game.getCurrentPlayerId());
        Mockito.verify(gameEventPublisher, Mockito.times(1)).publishFilteredState(game);
    }

    @Test
    void applySpecialPeek_opponentPeek_reveals_staysInAbilityPhase() {
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

        assertTrue(hand2.get(2).getVisibility());
        assertEquals(GameStatus.ABILITY_PEEK_OPPONENT, game.getStatus());
        assertEquals(1L, game.getCurrentPlayerId());
        Mockito.verify(gameEventPublisher, Mockito.times(1)).publishFilteredState(game);
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
    }

    // #64: draw from discard then swap drawn card so the top discard lands in playerHands (two service calls)
    @Test
    void moveDrawFromDiscardPile_success_discardTopEndsUpInHandAfterSwapDrawnCard() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card top = new Card();
        top.setValue(9);
        top.setCode("9H");

        Card handP1CardIndex0 = new Card();
        handP1CardIndex0.setValue(3);
        handP1CardIndex0.setCode("3C");

        List<Card> handP1 = new ArrayList<>();
        handP1.add(handP1CardIndex0);
        for (int i = 0; i < 3; i++) {
            handP1.add(new Card());
        }
        List<Card> handP2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            handP2.add(new Card());
        }

        Game game = new Game();
        game.setId("g-discard-draw");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setDiscardPile(new ArrayList<>(List.of(top)));
        game.setDrawnCard(null);
        game.setPlayerHands(new HashMap<>(Map.of(1L, handP1, 2L, handP2)));

        Mockito.when(gameRepository.findById("g-discard-draw")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        gameService.moveDrawFromDiscardPile("g-discard-draw", "token");

        assertTrue(game.getDiscardPile().isEmpty());
        assertNotNull(game.getDrawnCard());
        assertEquals(9, game.getDrawnCard().getValue());

        gameService.moveSwapDrawnCard("g-discard-draw", "token", 0);

        assertNull(game.getDrawnCard());
        assertEquals(9, handP1.get(0).getValue());
        assertEquals("9H", handP1.get(0).getCode());
        assertFalse(handP1.get(0).getVisibility());
        assertEquals(1, game.getDiscardPile().size());
        Card newTop = game.getDiscardPile().get(game.getDiscardPile().size() - 1);
        assertEquals(3, newTop.getValue());
        assertEquals("3C", newTop.getCode());
        assertTrue(newTop.getVisibility());
        assertEquals(2L, game.getCurrentPlayerId());

        Mockito.verify(gameRepository, Mockito.times(3)).save(game);
        Mockito.verify(gameEventPublisher, Mockito.times(3)).publishFilteredState(game);
    }

    @Test
    void moveDrawFromDiscardPile_emptyDiscard_throwsConflict() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Game game = new Game();
        game.setId("g-discard-empty");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setDiscardPile(new ArrayList<>());
        game.setOrderedPlayerIds(List.of(1L, 2L));

        Mockito.when(gameRepository.findById("g-discard-empty")).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.moveDrawFromDiscardPile("g-discard-empty", "token"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Discard pile is empty.", ex.getReason());
    }

    @Test
    void moveDrawFromDiscardPile_alreadyHasDrawnCard_throwsConflict() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card top = new Card();
        Game game = new Game();
        game.setId("g-discard-double");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setDiscardPile(new ArrayList<>(List.of(top)));
        // this will cause conflict
        game.setDrawnCard(new Card());

        Mockito.when(gameRepository.findById("g-discard-double")).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.moveDrawFromDiscardPile("g-discard-double", "token"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("You have already drawn a card!", ex.getReason());
    }

    @Test
    void moveDrawFromDiscardPile_notRoundActive_throwsConflict() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card top = new Card();
        Game game = new Game();
        game.setId("g-discard-phase");
        game.setStatus(GameStatus.INITIAL_PEEK);
        game.setCurrentPlayerId(1L);
        game.setDiscardPile(new ArrayList<>(List.of(top)));
        game.setOrderedPlayerIds(List.of(1L, 2L));

        Mockito.when(gameRepository.findById("g-discard-phase")).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.moveDrawFromDiscardPile("g-discard-phase", "token"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Round is not active.", ex.getReason());
    }

    @Test
    void moveSwapWithDiscardPile_success_swapsTopWithHandAndAdvancesTurn() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card fromDiscard = new Card();
        fromDiscard.setValue(7);
        fromDiscard.setCode("7D");
        Card handSlot = new Card();
        handSlot.setValue(3);
        handSlot.setCode("3C");

        List<Card> p1 = new ArrayList<>();
        p1.add(handSlot);
        for (int i = 0; i < 3; i++) {
            p1.add(new Card());
        }
        List<Card> p2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            p2.add(new Card());
        }
        Game game = new Game();
        game.setId("g-swap-discard");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setDiscardPile(new ArrayList<>(List.of(fromDiscard)));
        game.setDrawnCard(null);
        game.setPlayerHands(new HashMap<>(Map.of(1L, p1, 2L, p2)));

        Mockito.when(gameRepository.findById("g-swap-discard")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        gameService.moveSwapWithDiscardPile("g-swap-discard", "token", 0);

        // swapped directly with hand, drawn card slot not used
        assertNull(game.getDrawnCard());
        assertEquals(7, p1.get(0).getValue());
        assertEquals("7D", p1.get(0).getCode());
        assertFalse(p1.get(0).getVisibility());
        assertEquals(1, game.getDiscardPile().size());
        // get top card from discard pile
        // do not use getDiscardPileTopCard() to isolate behavior
        Card newTop = game.getDiscardPile().get(game.getDiscardPile().size() - 1);
        assertEquals(3, newTop.getValue());
        assertTrue(newTop.getVisibility());
        assertEquals(2L, game.getCurrentPlayerId());
        Mockito.verify(gameRepository, Mockito.times(2)).save(game);
        Mockito.verify(gameEventPublisher, Mockito.times(2)).publishFilteredState(game);
    }

    // #67: tests broadcast mapper in combination with real service logic execution
    @Test
    void moveSwapWithDiscardPile_broadcast_showsDiscardTopToAllAndHidesSwappedInHandCard() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card fromDiscard = new Card();
        fromDiscard.setValue(7);
        fromDiscard.setCode("7D");
        Card handSlot = new Card();
        handSlot.setValue(3);
        handSlot.setCode("3C");

        List<Card> p1 = new ArrayList<>();
        p1.add(handSlot);
        for (int i = 0; i < 3; i++) {
            p1.add(new Card());
        }
        List<Card> p2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            p2.add(new Card());
        }
        Game game = new Game();
        game.setId("g-swap-broadcast-chain");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setDiscardPile(new ArrayList<>(List.of(fromDiscard)));
        game.setDrawnCard(null);
        game.setPlayerHands(new HashMap<>(Map.of(1L, p1, 2L, p2)));

        Mockito.when(gameRepository.findById("g-swap-broadcast-chain")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        gameService.moveSwapWithDiscardPile("g-swap-broadcast-chain", "token", 0);

        GameStateBroadcastMapper broadcastMapper = new GameStateBroadcastMapper(lobbyService);
        for (Long viewerId : List.of(1L, 2L)) {
            GameStateBroadcastDTO dto = broadcastMapper.toBroadcastForViewer(game, viewerId);
            assertNotNull(dto.getDiscardPileTop());
            assertEquals(3, dto.getDiscardPileTop().getValue());
            assertEquals("3C", dto.getDiscardPileTop().getCode());
            CardViewDTO p1slot0 = dto.getPlayers().stream()
                    .filter(ph -> ph.getUserId() == 1L)
                    .findFirst()
                    .orElseThrow()
                    .getCards()
                    .get(0);
            assertTrue(p1slot0.isFaceDown());
            assertNull(p1slot0.getValue());
            assertNull(p1slot0.getCode());
        }
    }

    // #67: draw + swap drawn card — same behavior as moveSwapWithDiscardPile_broadcast_showsDiscardTopToAllAndHidesSwappedInHandCard
    @Test
    void moveDrawFromDiscardPile_thenSwapDrawnCard_broadcast_showsDiscardTopToAllAndHidesSwappedInHandCard() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card fromDiscard = new Card();
        fromDiscard.setValue(7);
        fromDiscard.setCode("7D");
        Card handSlot = new Card();
        handSlot.setValue(3);
        handSlot.setCode("3C");

        List<Card> p1 = new ArrayList<>();
        p1.add(handSlot);
        for (int i = 0; i < 3; i++) {
            p1.add(new Card());
        }
        List<Card> p2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            p2.add(new Card());
        }
        Game game = new Game();
        game.setId("g-draw-swap-broadcast-chain");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setDiscardPile(new ArrayList<>(List.of(fromDiscard)));
        game.setDrawnCard(null);
        game.setPlayerHands(new HashMap<>(Map.of(1L, p1, 2L, p2)));

        Mockito.when(gameRepository.findById("g-draw-swap-broadcast-chain")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        gameService.moveDrawFromDiscardPile("g-draw-swap-broadcast-chain", "token");
        gameService.moveSwapDrawnCard("g-draw-swap-broadcast-chain", "token", 0);

        GameStateBroadcastMapper broadcastMapper = new GameStateBroadcastMapper(lobbyService);
        for (Long viewerId : List.of(1L, 2L)) {
            GameStateBroadcastDTO dto = broadcastMapper.toBroadcastForViewer(game, viewerId);
            assertNotNull(dto.getDiscardPileTop());
            assertEquals(3, dto.getDiscardPileTop().getValue());
            assertEquals("3C", dto.getDiscardPileTop().getCode());
            CardViewDTO p1slot0 = dto.getPlayers().stream()
                    .filter(ph -> ph.getUserId() == 1L)
                    .findFirst()
                    .orElseThrow()
                    .getCards()
                    .get(0);
            assertTrue(p1slot0.isFaceDown());
            assertNull(p1slot0.getValue());
            assertNull(p1slot0.getCode());
        }
    }

    @Test
    void moveSwapWithDiscardPile_drawnCardAlreadySet_throwsConflict() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card top = new Card();
        Game game = new Game();
        game.setId("g-swap-block");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setDiscardPile(new ArrayList<>(List.of(top)));
        game.setDrawnCard(new Card());
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            hand.add(new Card());
        }
        game.setPlayerHands(Map.of(1L, hand));

        Mockito.when(gameRepository.findById("g-swap-block")).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.moveSwapWithDiscardPile("g-swap-block", "token", 0));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Cannot swap with discard pile after drawing a card", ex.getReason());
    }

    @Test
    void moveSwapWithDiscardPile_emptyDiscard_throwsConflict() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Game game = new Game();
        game.setId("g-swap-empty");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setDiscardPile(new ArrayList<>());
        game.setDrawnCard(null);
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            hand.add(new Card());
        }
        game.setPlayerHands(Map.of(1L, hand));

        Mockito.when(gameRepository.findById("g-swap-empty")).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.moveSwapWithDiscardPile("g-swap-empty", "token", 0));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Discard pile is empty", ex.getReason());
    }

    @Test
    void moveSwapWithDiscardPile_invalidIndex_throwsBadRequest() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card top = new Card();
        Game game = new Game();
        game.setId("g-swap-bad-index");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setDiscardPile(new ArrayList<>(List.of(top)));
        game.setDrawnCard(null);
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            hand.add(new Card());
        }
        game.setPlayerHands(Map.of(1L, hand));

        Mockito.when(gameRepository.findById("g-swap-bad-index")).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.moveSwapWithDiscardPile("g-swap-bad-index", "token", 10));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid card index", ex.getReason());
    }

    // #76: 30s ability timeout ends peek phase and passes turn when player never picks a target
    @Test
    void abilityTimeout_endsPeekAbility_andAdvancesTurn() {
        Game game = new Game();
        game.setId("g-ability-timeout-peek");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setDiscardPile(new ArrayList<>());
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);
        game.setPlayerHands(new HashMap<>(Map.of(
                1L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card())),
                2L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card()))
        )));

        // findById -> return game
        when(gameRepository.findById("g-ability-timeout-peek")).thenReturn(Optional.of(game));
        // save game -> return game
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(gameRepository).flush();

        List<Runnable> listOfScheduled = new ArrayList<>();
        // schedule call returns a future object, so mock them 
        ScheduledFuture<?> abilitySchedFuture = Mockito.mock(ScheduledFuture.class);
        ScheduledFuture<?> turnSchedFuture = Mockito.mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(inv -> {
            // add whatever we are scheduling to the list
            listOfScheduled.add(inv.getArgument(0));
            // scheduleCallCount is an instance variable in test class, initialized to 0 for each new test
            int n = scheduleCallCount++;
            // first -> ability timer, second -> turn timer
            return n == 0 ? abilitySchedFuture : turnSchedFuture;
        });

        Card fromDeck = new Card();
        fromDeck.setValue(8);
        game.setDrawnCard(fromDeck);
        game.setDrawnFromDeck(true);
        // schedules first timer (ability timer)
        gameService.moveCardToDiscardPile("g-ability-timeout-peek");

        assertEquals(GameStatus.ABILITY_PEEK_SELF, game.getStatus());
        assertEquals(1L, game.getCurrentPlayerId());
        assertEquals(1, listOfScheduled.size(), "only ability timeout should be scheduled so far");

        // simulate 30s timeout of ability timer
        // this ends the ability, advances the turn, and schedules the turn timer
        listOfScheduled.get(0).run();

        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());
    }

    // #76: 30s ability timeout ends swap phase and passes turn when player never swaps
    @Test
    void abilityTimeout_endsSwapAbility_andAdvancesTurn() {
        Game game = new Game();
        game.setId("g-ability-timeout-swap");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setDiscardPile(new ArrayList<>());
        game.setOrderedPlayerIds(List.of(1L, 2L, 3L));
        game.setCurrentPlayerId(1L);
        game.setPlayerHands(new HashMap<>(Map.of(
                1L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card())),
                2L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card())),
                3L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card()))
        )));

        // findById -> return game
        when(gameRepository.findById("g-ability-timeout-swap")).thenReturn(Optional.of(game));
        // save game -> return game
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(gameRepository).flush();

        List<Runnable> listOfScheduled = new ArrayList<>();
        // schedule call returns a future object, so mock them 
        ScheduledFuture<?> abilitySchedFuture = Mockito.mock(ScheduledFuture.class);
        ScheduledFuture<?> turnSchedFuture = Mockito.mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(inv -> {
            // add whatever we are scheduling to the list
            listOfScheduled.add(inv.getArgument(0));
            // scheduleCallCount is an instance variable in test class, initialized to 0 for each new test
            int n = scheduleCallCount++;
            // first -> ability timer, second -> turn timer
            return n == 0 ? abilitySchedFuture : turnSchedFuture;
        });

        Card fromDeck = new Card();
        fromDeck.setValue(11);
        game.setDrawnCard(fromDeck);
        game.setDrawnFromDeck(true);
        // schedules first timer (ability timer)
        gameService.moveCardToDiscardPile("g-ability-timeout-swap");

        assertEquals(GameStatus.ABILITY_SWAP, game.getStatus());
        assertEquals(1L, game.getCurrentPlayerId());
        assertEquals(1, listOfScheduled.size());

        // simulates ability timer timeout
        // this ends the ability, advances the turn, and schedules the turn timer
        listOfScheduled.get(0).run();

        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());
    }

    @Test
    void outdatedAbilityTimer_isIgnored_afterNewAbilityCycleStarts() {
        User p1 = new User();
        p1.setId(1L);
        User p2 = new User();
        p2.setId(2L);
        when(userRepository.findByToken("token-1")).thenReturn(p1);
        when(userRepository.findByToken("token-2")).thenReturn(p2);

        Game game = new Game();
        game.setId("g-outdated-ability-timer");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);
        game.setDrawPile(new ArrayList<>(List.of(new Card(), new Card(), new Card())));
        game.setDiscardPile(new ArrayList<>());
        game.setPlayerHands(new HashMap<>(Map.of(
                1L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card())),
                2L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card()))
        )));

        when(gameRepository.findById("g-outdated-ability-timer")).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(gameRepository).flush();

        List<Runnable> listOfScheduled = new ArrayList<>();
        // schedule call returns a future object, so mock them 
        ScheduledFuture<?> firstAbilityFuture = Mockito.mock(ScheduledFuture.class);
        ScheduledFuture<?> turnFuture = Mockito.mock(ScheduledFuture.class);
        ScheduledFuture<?> secondAbilityFuture = Mockito.mock(ScheduledFuture.class);

        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(inv -> {
            // add whatever we are scheduling to the list
            listOfScheduled.add(inv.getArgument(0));
            // scheduleCallCount is an instance variable in test class, initialized to 0 for each new test
            int n = scheduleCallCount++;
            if (n == 0) return firstAbilityFuture; // ability timer from first cycle
            if (n == 1) return turnFuture; // turn timer after skip
            if (n == 2) return secondAbilityFuture; // ability timer from second cycle
            return Mockito.mock(ScheduledFuture.class);
        });

        Card firstAbilityCard = new Card();
        firstAbilityCard.setValue(7);
        game.setDrawnCard(firstAbilityCard);
        game.setDrawnFromDeck(true);
        // schedule first ability timer
        gameService.moveCardToDiscardPile("g-outdated-ability-timer");
        assertEquals(GameStatus.ABILITY_PEEK_SELF, game.getStatus());

        // cancel first ability timer, schedule turn timer
        gameService.skipAbility("g-outdated-ability-timer", "token-1");
        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());
        // verify the first ability timer was canceled
        verify(firstAbilityFuture).cancel(anyBoolean());

        Card secondAbilityCard = new Card();
        secondAbilityCard.setValue(9);
        game.setDrawnCard(secondAbilityCard);
        game.setDrawnFromDeck(true);
        // cancel turn timer, schedule second ability timer
        gameService.moveCardToDiscardPile("g-outdated-ability-timer");
        assertEquals(GameStatus.ABILITY_PEEK_OPPONENT, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());

        // run outdated ability timer
        Runnable outdatedAbilityTimer = listOfScheduled.get(0);
        outdatedAbilityTimer.run();

        // nothing changes after trying to run an outdated ability timer
        assertEquals(GameStatus.ABILITY_PEEK_OPPONENT, game.getStatus());
        assertEquals(2L, game.getCurrentPlayerId());
    }

    @Test
    public void moveDrawFromDrawPile_emptyDrawPile_triggersAPIShuffleAndDrawsCard() {

        Game game = new Game();
        game.setId("gameId");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setDeckApiId("testId");
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);
        game.setDiscardPile(new ArrayList<>());

        Card bottom1 = new Card();
        bottom1.setCode("2H");
        Card bottom2 = new Card();
        bottom2.setCode("3C");
        Card bottom3 = new Card();
        bottom3.setCode("AS");
        game.setDiscardPile(new ArrayList<>(List.of(bottom1, bottom2, bottom3)));

        when(gameRepository.findById("gameId")).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(deckOfCardsAPIService).returnDrawnCardsToDeck(eq("testId"), anyList());
        doNothing().when(deckOfCardsAPIService).shuffleDeck(eq("testId"));
        
        CardDTO fresh1 = new CardDTO();
        fresh1.setCode("2H");
        CardDTO fresh2 = new CardDTO();
        fresh2.setCode("3C");
        when(deckOfCardsAPIService.drawFromDeck(eq("testId"), eq(2))).thenReturn(List.of(fresh1, fresh2));

        gameService.moveDrawFromDrawPile("gameId");

        verify(deckOfCardsAPIService).returnDrawnCardsToDeck(eq("testId"), anyList());
        verify(deckOfCardsAPIService).shuffleDeck(eq("testId"));
        verify(deckOfCardsAPIService).drawFromDeck(eq("testId"), eq(2));

        assertEquals(1, game.getDiscardPile().size(), "Discard pile should have 1 card left");
        assertEquals("AS", game.getDiscardPile().get(0).getCode());

        assertNotNull(game.getDrawnCard(), "A card should have been drawn");
        assertEquals("2H", game.getDrawnCard().getCode());

        assertEquals(1, game.getDrawPile().size(), "Draw pile should have 1 card left");
    }

    // drawing during INITIAL_PEEK phase is blocked
    @Test
    void moveDrawFromDrawPile_duringInitialPeek_throwsConflict() {
        Game game = new Game();
        game.setId("g-peek-block");
        game.setStatus(GameStatus.INITIAL_PEEK);
        game.setDrawPile(new ArrayList<>(List.of(new Card())));
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);

        Mockito.when(gameRepository.findById("g-peek-block")).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.moveDrawFromDrawPile("g-peek-block"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Cannot draw a card right now", ex.getReason());
    }

    // ability swap exchanges cards between two players without revealing values
    @Test
    void moveAbilitySwap_success_exchangesCardsWithoutRevealingValues() {
        User user = new User();
        user.setId(1L);
        Mockito.when(userRepository.findByToken("token")).thenReturn(user);

        Card ownCard = new Card();
        ownCard.setValue(3);
        ownCard.setCode("3C");
        ownCard.setVisibility(false);

        Card targetCard = new Card();
        targetCard.setValue(9);
        targetCard.setCode("9H");
        targetCard.setVisibility(false);

        List<Card> ownHand = new ArrayList<>();
        ownHand.add(ownCard);
        for (int i = 0; i < 3; i++) ownHand.add(new Card());

        List<Card> targetHand = new ArrayList<>();
        targetHand.add(targetCard);
        for (int i = 0; i < 3; i++) targetHand.add(new Card());

        Game game = new Game();
        game.setId("g-ability-swap");
        game.setStatus(GameStatus.ABILITY_SWAP);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setPlayerHands(new HashMap<>(Map.of(1L, ownHand, 2L, targetHand)));
        game.setDiscardPile(new ArrayList<>());

        Mockito.when(gameRepository.findById("g-ability-swap")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        gameService.moveAbilitySwap("g-ability-swap", "token", 0, 2L, 0);

        // cards should be swapped
        assertEquals("9H", ownHand.get(0).getCode());
        assertEquals("3C", targetHand.get(0).getCode());
        // neither card should be revealed
        assertFalse(ownHand.get(0).getVisibility());
        assertFalse(targetHand.get(0).getVisibility());
        // game should return to ROUND_ACTIVE
        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
        // turn should advance
        assertEquals(2L, game.getCurrentPlayerId());
    }

    // Cabo is called, exactly N-1 players get a final turn then rematch/no-rematch decision starts
    @Test
    void moveCallCabo_thenAllOtherPlayersTakeTurn_entersRematchDecision() {
        User caboUser = new User();
        caboUser.setId(1L);
        Mockito.when(userRepository.findByToken("token-1")).thenReturn(caboUser);

        Game game = new Game();
        game.setId("g-cabo");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(1L);
        game.setOrderedPlayerIds(new ArrayList<>(List.of(1L, 2L, 3L, 4L)));
        game.setDiscardPile(new ArrayList<>());
        game.setDrawPile(new ArrayList<>());
        game.setPlayerHands(new HashMap<>(Map.of(
                1L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card())),
                2L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card())),
                3L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card())),
                4L, new ArrayList<>(List.of(new Card(), new Card(), new Card(), new Card()))
        )));

        Mockito.when(gameRepository.findById("g-cabo")).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(gameRepository).flush();

        // player 1 calls cabo
        gameService.moveCallCabo("g-cabo", "token-1");
        assertTrue(game.isCaboCalled());
        assertEquals(1L, game.getCaboCalledByUserId());
        assertEquals(2L, game.getCurrentPlayerId()); // turn advances to player 2

        // player 2 takes final turn
        gameService.advanceTurnToNextPlayer("g-cabo");
        assertEquals(3L, game.getCurrentPlayerId()); // turn advances to player 3

        // player 3 takes final turn
        gameService.advanceTurnToNextPlayer("g-cabo");
        assertEquals(4L, game.getCurrentPlayerId()); // turn advances to player 4

        // player 4 takes final turn — next would be player 1 (who called cabo) so round ends
        gameService.advanceTurnToNextPlayer("g-cabo");
        assertEquals(GameStatus.ROUND_AWAITING_REMATCH, game.getStatus());
    }

    // reshuffle when draw pile is empty
    @Test
    void moveDrawFromDrawPile_emptyPile_triggersReshuffle() {
        Game game = new Game();
        game.setId("g-reshuffle");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setDrawPile(new ArrayList<>()); // Empty pile triggers reshuffle logic
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);

        // Create a card that WILL be in the draw pile after reshuffle
        Card cardAfterReshuffle = new Card();
        cardAfterReshuffle.setCode("2H");
        cardAfterReshuffle.setValue(2);

        // Mock behavior: 
        // 1. First call to findById returns empty draw pile game
        // 2. We mock the reshuffle (you can verify it was called if you want)
        // 3. We simulate the "reload" by having the second call return a game with cards
        Mockito.when(gameRepository.findById("g-reshuffle")).thenAnswer(new org.mockito.stubbing.Answer<Optional<Game>>() {
            private int count = 0;
            public Optional<Game> answer(org.mockito.invocation.InvocationOnMock invocation) {
                if (count++ == 0) return Optional.of(game); // First call (empty)
                game.setDrawPile(new ArrayList<>(List.of(cardAfterReshuffle))); // Simulate reshuffle result
                return Optional.of(game); // Second call (filled)
            }
        });

        gameService.moveDrawFromDrawPile("g-reshuffle");

        // Verify the card was drawn and set as the current drawn card
        assertNotNull(game.getDrawnCard());
        assertEquals("2H", game.getDrawnCard().getCode());
        assertTrue(game.isDrawnFromDeck());
    }

    // swap discard pile only on turn
    @Test
    void moveSwapWithDiscard_notCurrentPlayer_throwsForbidden() {
        Game game = new Game();
        game.setId("g-turn-check");
        game.setCurrentPlayerId(1L); // It is Player 1's turn
    
        User player2 = new User();
        player2.setId(2L);
        Mockito.when(userRepository.findByToken("token-p2")).thenReturn(player2);
        Mockito.when(gameRepository.findById("g-turn-check")).thenReturn(Optional.of(game));

        // Player 2 tries to move
        assertThrows(ResponseStatusException.class, 
            () -> gameService.moveSwapWithDiscardPile("g-turn-check", "token-p2", 0));
    }

    // abilities only tringger from draw pile
    @Test
    void moveSwapWithDiscard_abilityCard_doesNotTriggerAbility() {
        String gameId = "g1";
        String userToken = "token-123";
        Long userId = 1L;

        // 1. Setup User: Token must match and ID must match the Game's current player
        User user = new User();
        user.setId(userId);
        user.setToken(userToken);
        Mockito.when(userRepository.findByToken(userToken)).thenReturn(user);

        // 2. Setup Game: CurrentPlayerId must match the User's ID
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setCurrentPlayerId(userId);
        game.setOrderedPlayerIds(new ArrayList<>(List.of(userId, 2L)));
        game.setDrawnCard(null); // Satisfy Guard 2

        // Create a 7 (Ability Card)
        Card seven = new Card();
        seven.setCode("7H");
        seven.setValue(7);

        // Setup piles: Discard pile needs a card to satisfy Guard 3
        game.setDiscardPile(new ArrayList<>(List.of(seven)));
    
        // Setup player hand
        List<Card> hand = new ArrayList<>(List.of(new Card()));
        game.setPlayerHands(new HashMap<>(Map.of(userId, hand)));

        // 3. Mock Repositories
        Mockito.when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        Mockito.when(gameRepository.save(Mockito.any())).thenReturn(game);

        // 4. Execute
        gameService.moveSwapWithDiscardPile(gameId, userToken, 0);

        // 5. Verify: Even though a '7' was put on the discard pile, 
        // status should NOT change to a PEEK state because swaps don't trigger abilities.
        assertEquals(GameStatus.ROUND_ACTIVE, game.getStatus());
    }

    // test that initial peek timer transitions to ROUND_ACTIVE state and assigns a random player as starter
    @Test
    public void startGame_peekingTimerCompletes_transitionsToRoundActiveAndPicksRandomStarter() {

        when(gameSettings.getMinPlayers()).thenReturn(2);
        when(gameSettings.getMaxPlayers()).thenReturn(4);
        when(gameSettings.getStarterCardsPerPlayer()).thenReturn(4);
        when(gameSettings.getInitialPeekSeconds()).thenReturn(5L); // The 5-second timer
        when(gameSettings.getTurnSeconds()).thenReturn(30L); // Needed for the turn timer

        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            if (g.getId() == null) g.setId("test-game-id");
            return g;
        });

        List<Runnable> scheduledTasks = new ArrayList<>();
        ScheduledFuture<?> mockFuture = Mockito.mock(ScheduledFuture.class);

        when(scheduler.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS))).thenAnswer(inv -> {
            scheduledTasks.add(inv.getArgument(0));
            return mockFuture;
        });

        List<Long> playerIds = List.of(1L, 2L, 3L);
        Game startedGame = gameService.startGame(playerIds);

        assertEquals(GameStatus.INITIAL_PEEK, startedGame.getStatus(), "Game should start in INITIAL_PEEK");
        assertTrue(scheduledTasks.size() >= 1, "The peeking timer should be scheduled");

        when(gameRepository.findById("test-game-id")).thenReturn(Optional.of(startedGame));
        
        scheduledTasks.get(0).run();

        assertEquals(GameStatus.ROUND_ACTIVE, startedGame.getStatus(), "Game should transition to ROUND_ACTIVE");
        
        assertTrue(playerIds.contains(startedGame.getCurrentPlayerId()), "A random player should be assigned the first turn");
        
    }

    // test whether opponents get null for drawn card to prevent cheating
    @Test
    public void getDrawnCard_opponentRequests_returnsNull() {
        String snooperToken = "player2-token";
        User opponent = new User(); 
        opponent.setId(2L); 
        opponent.setToken(snooperToken);
        
        Game game = new Game();
        game.setId("g-drawn-card-snoop");
        game.setCurrentPlayerId(1L);

        Card drawnCard = new Card();
        drawnCard.setCode("AS");
        game.setDrawnCard(drawnCard);

        when(userRepository.findByToken(snooperToken)).thenReturn(opponent);
        when(gameRepository.findById("g-drawn-card-snoop")).thenReturn(Optional.of(game));

        Card result = gameService.getDrawnCard("g-drawn-card-snoop", snooperToken);

        assertNull(result, "Opponents should receive null to prevent cheating");
    }

    // test that if a player times out without drawing, the game auto-draws a card for them and discards it, then advances the turn
    @Test
    public void executeTimoutMove_playerHasNotDrawnCard_autoDrawsAndDiscards() {

        Game game = new Game();
        game.setId("g-timeout-nodraw");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);
        game.setDrawnCard(null); // No card drawn yet

        Card topDraw = new Card(); topDraw.setCode("2H");
        game.setDrawPile(new ArrayList<>(List.of(topDraw)));
        game.setDiscardPile(new ArrayList<>());

        when(gameRepository.findById("g-timeout-nodraw")).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        gameService.executeTimoutMove("g-timeout-nodraw", 1L);

        assertEquals(0, game.getDrawPile().size(), "The card should be removed from the draw pile");
        assertEquals(1, game.getDiscardPile().size(), "The card should be added to the discard pile");
        
        Card discardedCard = game.getDiscardPile().get(0);
        assertEquals("2H", discardedCard.getCode(), "The discarded card should be the one from the draw pile");
        assertTrue(discardedCard.getVisibility(), "The discarded card must be face-up (visible)");
        
        assertNull(game.getDrawnCard(), "The drawn card slot should be empty again");

        assertEquals(2L, game.getCurrentPlayerId(), "Turn should advance to Player 2");
    }

    // tests that swapping a drawn card with a card in hand properly discards the swapped-out card face-up and hides the new card in hand, then advances the turn
    @Test
    public void moveSwapDrawnCard_validIndex_swapsCardsAndDiscardsFaceUp() {
        String token = "player1-token";
        User player1 = new User();
        player1.setId(1L);
        player1.setToken(token);

        Game game = new Game();
        game.setId("g-swap-drawn");
        game.setStatus(GameStatus.ROUND_ACTIVE);
        game.setOrderedPlayerIds(List.of(1L, 2L));
        game.setCurrentPlayerId(1L);

        Card handCard0 = new Card(); handCard0.setCode("2H");
        Card handCard1 = new Card(); handCard1.setCode("3C"); 
        List<Card> hand = new ArrayList<>(Arrays.asList(handCard0, handCard1));
        game.setPlayerHands(new HashMap<>(Map.of(1L, hand)));

        Card drawnCard = new Card(); drawnCard.setCode("AS");
        game.setDrawnCard(drawnCard);
        game.setDrawnFromDeck(true);

        game.setDiscardPile(new ArrayList<>());

        when(userRepository.findByToken(token)).thenReturn(player1);
        when(gameRepository.findById("g-swap-drawn")).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        gameService.moveSwapDrawnCard("g-swap-drawn", token, 1);

        List<Card> updatedHand = game.getPlayerHands().get(1L);
        assertEquals(2, updatedHand.size());
        assertEquals("AS", updatedHand.get(1).getCode(), "The drawn Ace of Spades should now be at index 1");
        assertFalse(updatedHand.get(1).getVisibility(), "The new card in hand must be face-down");

        assertEquals(1, game.getDiscardPile().size(), "The discard pile should now have 1 card");
        Card discardedCard = game.getDiscardPile().get(0);
        
        assertEquals("3C", discardedCard.getCode(), "The discarded card should be the 3 of Clubs removed from the hand");
        assertTrue(discardedCard.getVisibility(), "CRITICAL: The discarded card MUST be face-up!");

        assertNull(game.getDrawnCard(), "The drawn card slot should be cleared after the swap");
        assertEquals(2L, game.getCurrentPlayerId(), "Turn should successfully advance to Player 2");
    }



}
