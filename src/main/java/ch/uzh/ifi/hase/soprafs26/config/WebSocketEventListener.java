package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.service.DisconnectService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private final DisconnectService disconnectService;

    public WebSocketEventListener(DisconnectService disconnectService) {
        this.disconnectService = disconnectService;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        // extract token from connect headers
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token != null && !token.isBlank()) {
            disconnectService.handleDisconnect(token);
        }
    }
}