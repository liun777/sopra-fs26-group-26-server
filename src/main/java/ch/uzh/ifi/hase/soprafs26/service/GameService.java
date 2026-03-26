package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game startGame(List<Long> playerIds) {
        // create a new game
        Game newGame = new Game();

        // game logic comes here

        // save it to the DB
        Game savedGame = gameRepository.save(newGame);
        gameRepository.flush();
        return savedGame;
    }

}