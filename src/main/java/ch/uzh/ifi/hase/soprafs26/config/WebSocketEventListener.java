package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.service.DisconnectService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

@Component
public class WebSocketEventListener {

    private final DisconnectService disconnectService;

    public WebSocketEventListener(DisconnectService disconnectService) {
        this.disconnectService = disconnectService;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    
        // Get the ID we saved in the interceptor
        Long userId = (Long) accessor.getSessionAttributes().get("userId");
    
        if (userId != null) {
            // Tell your service to start the 60s timer for this specific User ID
            disconnectService.handleConnectionLoss(userId);
        }
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) accessor.getSessionAttributes().get("userId");

        if (userId != null) {
            // This stops the 60s timer if the user refreshes or reconnects
            disconnectService.cancelDisconnectTimer(userId);
        }
    }
}