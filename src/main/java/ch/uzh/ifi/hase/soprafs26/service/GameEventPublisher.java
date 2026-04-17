package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.AbilityRevealPayloadDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateBroadcastDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.GameStateBroadcastMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameEventPublisher {

    // client subscribes to /user/queue/game-state
    // based on the /user/ prefix Spring will handle per-user websocket communication
    public static final String USER_QUEUE_GAME_STATE = "/queue/game-state";

    // Client: /user/queue/ability-reveal 
    // durable notice for 7/8 and 9/10 peek
    public static final String USER_QUEUE_ABILITY_REVEAL = "/queue/ability-reveal";

    // used to send messages to client
    private final SimpMessagingTemplate messagingTemplate;
    // has the logic for filtering
    private final GameStateBroadcastMapper gameStateBroadcastMapper;

    public GameEventPublisher(SimpMessagingTemplate messagingTemplate,
                              GameStateBroadcastMapper gameStateBroadcastMapper) {
        this.messagingTemplate = messagingTemplate;
        this.gameStateBroadcastMapper = gameStateBroadcastMapper;
    }

    public void publishFilteredState(Game game) {
        if (game == null) {
            return;
        }
        // get all players of the game
        List<Long> playerIds = game.getOrderedPlayerIds();
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        // for each player
        for (Long userId : playerIds) {
            // build the player's filtered representation of the game state
            GameStateBroadcastDTO dto = gameStateBroadcastMapper.toBroadcastForViewer(game, userId);
            // send it to that player
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_QUEUE_GAME_STATE, dto);
        }
    }

    // sends the peeked card only to the acting player (same user id as current turn during ability)
    // avoids problems with instantly changed and re-broadcasted game state / no need for timer
    // follows current client design
    public void publishAbilityPeekReveal(Long userId, String gameId, GameStatus abilityPhase, Card peekedCard) {
        if (userId == null || gameId == null || peekedCard == null) {
            return;
        }
        String abilityType;
        if (abilityPhase == GameStatus.ABILITY_PEEK_SELF) {
            abilityType = "peek_self";
        } else if (abilityPhase == GameStatus.ABILITY_PEEK_OPPONENT) {
            abilityType = "peek_opponent";
        } else {
            return;
        }
        AbilityRevealPayloadDTO.RevealedCardItemDTO item = new AbilityRevealPayloadDTO.RevealedCardItemDTO();
        item.setValue(peekedCard.getValue());
        item.setCode(peekedCard.getCode());
        AbilityRevealPayloadDTO payload = new AbilityRevealPayloadDTO();
        payload.setGameId(gameId);
        payload.setAbilityType(abilityType);
        payload.setRevealedCards(List.of(item));
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_QUEUE_ABILITY_REVEAL, payload);
    }
}
