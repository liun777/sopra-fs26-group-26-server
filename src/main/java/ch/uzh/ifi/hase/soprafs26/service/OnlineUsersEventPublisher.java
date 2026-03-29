package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class OnlineUsersEventPublisher {

    private static final String TOPIC = "/topic/users/online";

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public OnlineUsersEventPublisher(SimpMessagingTemplate messagingTemplate,
                                     UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    /** Pushes current online users to subscribers. */
    public void broadcastOnlineUsers() {
        Runnable send = () -> {
            List<UserGetDTO> online = userRepository.findAll().stream()
                    .filter(u -> u.getStatus() == UserStatus.ONLINE)
                    .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                    .peek(d -> d.setToken(null))
                    .toList();
            messagingTemplate.convertAndSend(TOPIC, online);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
