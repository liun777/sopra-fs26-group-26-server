package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardViewDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DiscardTopDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateBroadcastDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerHandViewDTO;
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
        dto.setStatus(game.getStatus());
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

        Card drawnCard = game.getDrawnCard();
        if (drawnCard != null) {
            
            CardViewDTO drawnCardDTO = new CardViewDTO();
            // allow the user that drew the card to see its fields
            if (viewerUserId.equals(game.getCurrentPlayerId())) {
                drawnCardDTO.setValue(drawnCard.getValue());
                drawnCardDTO.setCode(drawnCard.getCode());
                drawnCardDTO.setFaceDown(false);
            }
            // everyone else is not allowed to see it
            else {
                drawnCardDTO.setValue(null);
                drawnCardDTO.setCode(null);
                drawnCardDTO.setFaceDown(true);
            }
            dto.setDrawnCard(drawnCardDTO);
        }

        Map<Long, List<Card>> hands = game.getPlayerHands();
        // if hands is null -> empty map
        if (hands == null) {
            hands = Map.of();
        }
        List<Long> ordered = game.getOrderedPlayerIds();
        // if null -> no hands filled 
        if (ordered == null) {
            ordered = List.of();
        }

        List<PlayerHandViewDTO> playerHands = new ArrayList<>();
        for (Long ownerId : ordered) {
            PlayerHandViewDTO handView = new PlayerHandViewDTO();
            handView.setUserId(ownerId);
            // if hands is empty map or no key in map for this player -> empty list of cards
            List<Card> hand = hands.getOrDefault(ownerId, List.of());
            List<CardViewDTO> views = new ArrayList<>();
            for (int i = 0; i < hand.size(); i++) {
                views.add(toCardView(i, hand.get(i), ownerId, viewerUserId, game.getCurrentPlayerId()));
            }
            handView.setCards(views);
            playerHands.add(handView);
        }
        dto.setPlayers(playerHands);
        return dto;
    }

    private CardViewDTO toCardView(int position, Card card, Long handOwnerId, Long viewerUserId, Long currentPlayerId) {
        // create a representation for the card
        CardViewDTO v = new CardViewDTO();
        v.setPosition(position);
        boolean isOwner = handOwnerId.equals(viewerUserId);
        boolean isViewerCurrentPlayer = viewerUserId.equals(currentPlayerId);

        // LOGIK-UPDATE:
        // Zeige Karte, wenn:
        // 1. Der Betrachter der Besitzer ist UND die Karte sichtbar ist (Initial Peek / 7,8)
        // 2. ODER: Der Betrachter NICHT der Besitzer ist, aber gerade dran ist UND die Karte sichtbar ist (9,10)
        boolean canSee = (isOwner && card.getVisibility()) || (!isOwner && isViewerCurrentPlayer && card.getVisibility());

        v.setFaceDown(!canSee);
        if (canSee) {
            v.setValue(card.getValue());
            v.setCode(card.getCode());
        } else {
            v.setValue(null);
            v.setCode(null);
        }
        return v;
    }
}
