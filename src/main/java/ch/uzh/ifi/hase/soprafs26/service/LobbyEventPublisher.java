package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class LobbyEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public LobbyEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcasts the full updated lobby state to all subscribers.
     * Call this whenever the lobby changes (player joins, leaves, game starts, etc.)
     *
     * @param lobbyId the lobby's unique ID
     * @param payload any object — will be serialized to JSON automatically
     */
    public void broadcastLobbyUpdate(Long lobbyId, Object payload) {
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, payload);
        if (payload instanceof Lobby lobby && lobby.getSessionId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/lobby/session/" + lobby.getSessionId(), payload);
        }
    }
}