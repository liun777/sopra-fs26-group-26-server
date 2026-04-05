package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardViewDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DiscardTopDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateBroadcastDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerBoardViewDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// builds a game state representation with filtered data for a given player
@Component
public class GameStateBroadcastMapper {

    public GameStateBroadcastDTO toBroadcastForViewer(Game game, Long viewerUserId) {
        GameStateBroadcastDTO dto = new GameStateBroadcastDTO();

        dto.setGameId(game.getId());
        dto.setCurrentTurnUserId(game.getCurrentPlayerId());

        List<Card> draw = game.getDrawPile();
        dto.setDrawPileCount(draw == null ? 0 : draw.size());

        List<Card> discard = game.getDiscardPile();
        if (discard != null && !discard.isEmpty()) {
            Card top = discard.get(discard.size() - 1);
            DiscardTopDTO topDto = new DiscardTopDTO();
            topDto.setValue(top.getValue());
            topDto.setCode(top.getCode());
            dto.setDiscardPileTop(topDto);
        }

        Map<Long, List<Card>> hands = game.getPlayerHands();
        // if hands is null -> empty map
        if (hands == null) {
            hands = Map.of();
        }
        List<Long> ordered = game.getOrderedPlayerIds();
        // if null - no boards
        if (ordered == null) {
            ordered = List.of();
        }

        List<PlayerBoardViewDTO> boards = new ArrayList<>();
        for (Long ownerId : ordered) {
            PlayerBoardViewDTO board = new PlayerBoardViewDTO();
            board.setUserId(ownerId);
            // if hands is empty map or no key in map for this player -> empty list of cards
            List<Card> hand = hands.getOrDefault(ownerId, List.of());
            List<CardViewDTO> views = new ArrayList<>();
            for (int i = 0; i < hand.size(); i++) {
                views.add(toCardView(i, hand.get(i), ownerId, viewerUserId));
            }
            board.setCards(views);
            boards.add(board);
        }
        dto.setPlayers(boards);
        return dto;
    }

    private CardViewDTO toCardView(int position, Card card, Long handOwnerId, Long viewerUserId) {
        // create a representation for the card
        CardViewDTO v = new CardViewDTO();
        v.setPosition(position);
        boolean isOwner = handOwnerId.equals(viewerUserId);
        boolean show = isOwner && card.getVisibility();
        v.setFaceDown(!show);
        if (show) {
            v.setValue(card.getValue());
            v.setCode(card.getCode());
        } else {
            v.setValue(null);
            v.setCode(null);
        }
        return v;
    }
}
