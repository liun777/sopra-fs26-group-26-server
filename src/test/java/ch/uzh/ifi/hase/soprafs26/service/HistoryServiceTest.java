package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class HistoryServiceTest {
    
    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private HistoryService historyService;

    @Test
    public void requestSessionHistory_success() {

        // Setup User
        User testUser = new User();
        testUser.setId(1L);

        // Setup Session
        Session testSession = new Session();
        testSession.setId(10L); // Internal ID is now a Long
        testSession.setSessionId("testSessionId"); // The string identifier
        
        // Add the user to the scores map so it passes the filter in HistoryService
        testSession.getTotalScoreByUserId().put(1L, 150); 

        // Mock repository behavior
        Mockito.when(sessionRepository.findAll()).thenReturn(List.of(testSession));
        Mockito.when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // Execute service method
        List<Session> result = historyService.getUserSessionHistory(1L);

        // Assertions
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("testSessionId", result.get(0).getSessionId());
    }
}