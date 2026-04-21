package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final UserRepository userRepository;

    public StompAuthChannelInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // 1. Handle initial CONNECT: Validate token and store userId in session
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            String token = (authHeaders == null || authHeaders.isEmpty()) ? null : authHeaders.get(0).trim();

            if (token == null || token.isEmpty()) {
                throw new MessagingException("Missing Authorization token");
            }

            User user = userRepository.findByToken(token);
            if (user == null) {
                throw new MessagingException("Invalid Authorization token");
            }

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                throw new MessagingException("Missing websocket session attributes");
            }

            // Keep userId in websocket session attributes for connect/disconnect tracking.
            sessionAttributes.put("userId", user.getId());
            // Also set websocket principal so /user/queue routing can resolve convertAndSendToUser calls.
            accessor.setUser(new StompPrincipal(String.valueOf(user.getId())));
            
            // Set initial heartbeat
            user.setLastHeartbeat(Instant.now());
            userRepository.save(user);
        }

        // 2. Refresh Heartbeat: Resets the 5-minute idle clock every time they do anything
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        Long userId = null;
        if (sessionAttributes != null) {
            Object rawUserId = sessionAttributes.get("userId");
            if (rawUserId instanceof Long id) {
                userId = id;
            } else if (rawUserId instanceof Number id) {
                userId = id.longValue();
            }
        }
        if (userId != null) {
            updateUserHeartbeat(userId);
        }

        return message;
    }

    /**
     * Helper to update the lastHeartbeat timestamp in the database.
     * This keeps the "death clock" from starting while the user is active.
     */
    private void updateUserHeartbeat(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            // Optimization: You could check if lastHeartbeat was > 10s ago before saving 
            // to reduce DB load, but for now, this ensures the 5-min rule is safe.
            user.setLastHeartbeat(Instant.now());
            userRepository.save(user);
        });
    }
}
