package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbySettingsPatchDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WaitingLobbyViewDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class LobbyController {

    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    // POST /lobbies — create a new lobby
    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    public Lobby createLobby(@RequestHeader("Authorization") String token,
                             @RequestBody Map<String, Boolean> body) {
        Boolean isPublic = body.get("isPublic");
        return lobbyService.createLobby(token, isPublic);
    }

    // GET /lobbies — get all public lobbies
    @GetMapping("/lobbies")
    @ResponseStatus(HttpStatus.OK)
    public List<Lobby> getPublicLobbies(@RequestHeader("Authorization") String token) {
        return lobbyService.getPublicLobbies(token);
    }

    // POST /lobbies/{sessionId}/players — join a lobby
    @PostMapping("/lobbies/{sessionId}/players")
    @ResponseStatus(HttpStatus.OK)
    public Lobby joinLobby(@PathVariable String sessionId,
                           @RequestHeader("Authorization") String token) {
        return lobbyService.joinLobby(sessionId, token);
    }

    @PatchMapping("/lobbies/{sessionId}/settings")
    @ResponseStatus(HttpStatus.OK)
    public Lobby updateLobbySettings(@PathVariable String sessionId,
                                     @RequestHeader("Authorization") String token,
                                     @RequestBody LobbySettingsPatchDTO body) {
        return lobbyService.updateLobbySettings(token, sessionId, body);
    }

    @GetMapping("/lobbies/waiting/{sessionId}")
    @ResponseStatus(HttpStatus.OK)
    public WaitingLobbyViewDTO getWaitingLobby(@PathVariable String sessionId,
                                               @RequestHeader("Authorization") String token) {
        return lobbyService.getWaitingLobbyView(token, sessionId);
    }

    @GetMapping("/lobbies/my/waiting")
    @ResponseStatus(HttpStatus.OK)
    public Lobby getMyWaitingLobby(@RequestHeader("Authorization") String token) {
        return lobbyService.getMyWaitingLobbyAsHost(token);
    }
}