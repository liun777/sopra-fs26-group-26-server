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

            // Important: This is what allows the logic below to work!
            accessor.getSessionAttributes().put("userId", user.getId());
            
            // Set initial heartbeat
            user.setLastHeartbeat(Instant.now());
            userRepository.save(user);
        }

        // 2. Refresh Heartbeat: Resets the 5-minute idle clock every time they do anything
        Long userId = (Long) accessor.getSessionAttributes().get("userId");
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