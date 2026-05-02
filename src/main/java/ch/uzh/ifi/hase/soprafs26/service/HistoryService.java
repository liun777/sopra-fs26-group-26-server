package ch.uzh.ifi.hase.soprafs26.service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;


import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

@Service
public class HistoryService {
    
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public HistoryService(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    public List<Session> getUserSessionHistory(Long userId) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));
        
        List<Session> allSessions = sessionRepository.findAll();
        List<Session> filteredSessions = allSessions.stream()
            .filter(session->(session.getTotalScoreByUserId().containsKey(userId)))
            .collect(Collectors.toList());

        return filteredSessions;
    }
}
