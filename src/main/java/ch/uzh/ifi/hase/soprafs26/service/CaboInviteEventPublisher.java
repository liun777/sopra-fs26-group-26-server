package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.CaboInvitePendingDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CaboInviteSentDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CaboInviteEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public CaboInviteEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** After DB commit so subscribers refetch sees the row. */
    public void publishToInviteeAfterCommit(Long toUserId, CaboInvitePendingDTO dto) {
        Runnable send = () -> messagingTemplate.convertAndSend(
                "/topic/users/" + toUserId + "/invites", dto);
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

    /** Notifies the inviter when an invitee accepts or declines */
    public void publishToInviterAfterCommit(Long fromUserId, CaboInviteSentDTO dto) {
        Runnable send = () -> messagingTemplate.convertAndSend(
                "/topic/users/" + fromUserId + "/invites/sent", dto);
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
