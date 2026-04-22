package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class OnlineUsersEventPublisher {

    private static final String TOPIC = "/topic/users/online";

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private volatile String lastPresenceSnapshot = "";

    public OnlineUsersEventPublisher(SimpMessagingTemplate messagingTemplate,
                                     UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    /** Pushes current active users to subscribers (all non-offline statuses). */
    public void broadcastOnlineUsers() {
        Runnable send = () -> {
            List<UserGetDTO> online = userRepository.findByStatusNot(UserStatus.OFFLINE).stream()
                    .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                    .peek(d -> d.setToken(null))
                    .sorted(Comparator.comparing(UserGetDTO::getId, Comparator.nullsLast(Long::compareTo)))
                    .toList();
            String snapshot = buildSnapshot(online);
            if (snapshot.equals(lastPresenceSnapshot)) {
                return;
            }
            lastPresenceSnapshot = snapshot;
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

    private String buildSnapshot(List<UserGetDTO> online) {
        StringBuilder builder = new StringBuilder(online.size() * 24);
        for (UserGetDTO user : online) {
            builder.append(Objects.toString(user.getId(), ""))
                    .append('|')
                    .append(Objects.toString(user.getUsername(), ""))
                    .append('|')
                    .append(Objects.toString(user.getStatus(), ""))
                    .append(';');
        }
        return builder.toString();
    }
}
