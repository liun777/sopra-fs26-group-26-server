package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class LobbyWebSocketController {

    private final LobbyService lobbyService;

    public LobbyWebSocketController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @MessageMapping("/lobby/{lobbyId}/state")
    @SendTo("/topic/lobby/{lobbyId}")
    public Lobby getLobbyState(@DestinationVariable Long lobbyId) {
        return lobbyService.getLobbyById(lobbyId);
    }
}