package ch.uzh.ifi.hase.soprafs26.service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;


import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

@Service
public class HistoryService {
    
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public HistoryService(GameRepository gameRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    public List<Game> getUserGameHistory(Long userId) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));
        
        List<Game> allGames = gameRepository.findAll();
        List<Game> filteredGames = allGames.stream()
            .filter(game->(game.getOrderedPlayerIds().contains(userId)))
            .collect(Collectors.toList());

        return filteredGames;
    }
}
