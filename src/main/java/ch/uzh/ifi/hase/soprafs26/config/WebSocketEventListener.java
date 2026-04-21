package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.service.DisconnectService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    private final DisconnectService disconnectService;

    public WebSocketEventListener(DisconnectService disconnectService) {
        this.disconnectService = disconnectService;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        String sessionId = accessor.getSessionId();

        if (userId != null) {
            if (sessionId != null && !sessionId.isBlank()) {
                disconnectService.unregisterWebSocketSession(userId, sessionId);
            } else {
                // Fallback when broker message has no session id
                disconnectService.handleConnectionLoss(userId);
            }
        }
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        String sessionId = accessor.getSessionId();

        if (userId != null) {
            if (sessionId != null && !sessionId.isBlank()) {
                disconnectService.registerWebSocketSession(userId, sessionId);
            } else {
                // Fallback when broker message has no session id
                disconnectService.cancelDisconnectTimer(userId);
            }
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        if (accessor == null) {
            return null;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }

        Object raw = sessionAttributes.get("userId");
        if (raw instanceof Long userId) {
            return userId;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }

        if (raw instanceof String textId) {
            try {
                return Long.parseLong(textId.trim());
            } catch (NumberFormatException ignored) {
                // ignore and try principal fallback
            }
        }

        if (accessor.getUser() != null) {
            try {
                return Long.parseLong(accessor.getUser().getName());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
