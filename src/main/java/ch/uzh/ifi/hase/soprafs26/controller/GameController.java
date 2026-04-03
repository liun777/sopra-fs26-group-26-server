package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Card;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.entity.Card;

@RestController
public class GameController {

    private final GameService gameService;
    private final LobbyService lobbyService;

    public GameController(GameService gameService, LobbyService lobbyService) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
    }

    // endpoint according to REST interface
    @PostMapping("/lobbies/{sessionId}/start")
    @ResponseStatus(HttpStatus.OK)
    public Game startGame(  @RequestHeader("Authorization") String token,
                            @PathVariable String sessionId,
                            @RequestBody Map<String, Integer> requestBody) {
        
        // get lobby by the lobbyId, this assumes that sessionId and lobbyId are equivalent
        Lobby currentLobby = lobbyService.getLobbyBySessionId(sessionId);
        // retrieve playerIds of players currently in the lobby
        List<Long> playerIds = currentLobby.getPlayerIds();
        // return the game to the frontend
        return gameService.startGame(playerIds);
    }

    // Backlog #9: Implement logic to always render the DiscardPile top card with its face-up value
    @GetMapping("/games/{gameId}/discard-pile/top")
    @ResponseStatus(HttpStatus.OK)
    public Card getDiscardPileTopCard(@PathVariable String gameId) {
        return gameService.getDiscardPileTopCard(gameId);

    }


}