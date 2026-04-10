package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
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
}
