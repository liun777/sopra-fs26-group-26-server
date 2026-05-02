package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Optional;

import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class HistoryServiceTest {
    
    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private HistoryService historyService;

    @Test
    public void requestGameHistory_success() {

        User testUser = new User();
        testUser.setId(1L);

        Game testGame = new Game();
        testGame.setId("testId");
        testGame.setOrderedPlayerIds(List.of(1L, 2L, 3L));

        Mockito.when(gameRepository.findAll()).thenReturn(List.of(testGame));
        Mockito.when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        List<Game> result = historyService.getUserGameHistory(1L);

        assertEquals(1, result.size());
        assertEquals("testId", result.get(0).getId());
    }
}
