package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PeekSelectionDTO;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;

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
        Lobby currentLobby = lobbyService.verifyLobbyCanStart(token, sessionId);
        // retrieve playerIds of players currently in lobby
        List<Long> playerIds = currentLobby.getPlayerIds();
        Game startedGame = gameService.startGame(playerIds);
        lobbyService.markLobbyAsPlaying(sessionId);
        return startedGame;
    }

    // Backlog #9: Implement logic to always render the DiscardPile top card with its face-up value
    @GetMapping("/games/{gameId}/discard-pile/top")
    @ResponseStatus(HttpStatus.OK)
    public Card getDiscardPileTopCard(@PathVariable String gameId) {
        return gameService.getDiscardPileTopCard(gameId);

    }

    //# 8: Implement a global isMyTurn state that disables all buttons and click listeners on the game board when false.
    @GetMapping("/games/{gameId}/is-my-turn/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public boolean isMyTurn(
            @PathVariable String gameId,
            @PathVariable Long userId) {
        return gameService.isMyTurn(gameId, userId);
    }

    // get the players own hand aka only the visible cards are being shown
    @GetMapping("/games/{gameId}/my-hand")
    @ResponseStatus(HttpStatus.OK)
    public List<Card> getMyHand(
            @PathVariable String gameId,
            @RequestHeader("Authorization") String token) {
        return gameService.getMyHand(gameId, token);
    }

    // Example empty stubs of move endpoints to demonstrate the interceptor from #30

    @PostMapping("/games/{gameId}/moves/draw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveDrawFromDrawPile(
            @PathVariable String gameId,
            @RequestHeader("Authorization") String token) {
        gameService.moveDrawFromDrawPile(gameId);
    }

    // swap drawn card with one of the player's hand cards
    @PostMapping("/games/{gameId}/drawn-card/swap")
    @ResponseStatus(HttpStatus.OK)
    public void moveSwapDrawnCard(
            @PathVariable String gameId,
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Integer> body) {
        int targetCardIndex = body.get("targetCardIndex");
        gameService.moveSwapDrawnCard(gameId, token, targetCardIndex);
    }

    // swap top card of discard pile with one of the player's hand cards
    @PostMapping("/games/{gameId}/discard-pile/swap")
    @ResponseStatus(HttpStatus.OK)
    public void moveSwapWithDiscardPile(
            @PathVariable String gameId,
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Integer> body) {
        int targetCardIndex = body.get("targetCardIndex");
        gameService.moveSwapWithDiscardPile(gameId, token, targetCardIndex);
    }

    @PostMapping("/games/{gameId}/moves/cabo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveCallCabo(
            @PathVariable String gameId,
            @RequestHeader("Authorization") String token) {
        gameService.moveCallCabo(gameId);
    }

    // #47 and #49
    @PostMapping("/games/{gameId}/peek")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectPeekCards(
            @PathVariable String gameId,
            @RequestHeader("Authorization") String token,
            @RequestBody PeekSelectionDTO body) {
        gameService.applyPeek(gameId, token, body);
    }

}
