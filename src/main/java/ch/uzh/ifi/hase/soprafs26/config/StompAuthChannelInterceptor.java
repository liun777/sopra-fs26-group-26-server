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

import java.util.List;

// needed for authenticated communication in the scope of websocket's STOMP
// the broadcast of game's state will use authentication information for filtering of cards' information
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final UserRepository userRepository;

    public StompAuthChannelInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // this is run when the server receives a STOMP message from client
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // if it is not a CONNECT message - do nothing
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        // get authorization data from header
        List<String> headers = accessor.getNativeHeader("Authorization");
        // if no authorization header: connection is rejected, client gets no access
        String token = (headers == null || headers.isEmpty()) ? null : headers.get(0).trim();
        if (token == null || token.isEmpty()) {
            throw new MessagingException("Missing Authorization token on STOMP CONNECT");
        }
        // invalid token: also rejection
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new MessagingException("Invalid Authorization token on STOMP CONNECT");
        }
        // authorization passed - attach user's identity to this websocket connection
        // setUser needs an instance of type Principal
        accessor.setUser(new StompPrincipal(String.valueOf(user.getId())));
        return message;
    }
}
