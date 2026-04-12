package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


// integration tests for deck of cards api
// internet access required
class DeckOfCardsAPIServiceLiveTest {

    @Test
    void createShuffleAndDrawFullDeck_mapsKHKDToX1X2_returns52Cards() {
        DeckOfCardsAPIService service = new DeckOfCardsAPIService();
        //create new deck
        String deckId = service.createNewDeckId();
        //shuffle deck
        service.shuffleDeck(deckId);
        // draw all cards
        List<CardDTO> cards = service.drawFromDeck(deckId, 52);

        assertEquals(52, cards.size());
        // get card codes
        List<String> codes = cards.stream().map(CardDTO::getCode).toList();
        assertTrue(codes.contains("X1"));
        assertTrue(codes.contains("X2"));
        // KH must be mapped to X1 during draw
        assertFalse(codes.contains("KH"));
        // KD must be mapped to X2 during draw
        assertFalse(codes.contains("KD"));
        assertTrue(codes.contains("KS"));
        assertTrue(codes.contains("KC"));
    }

    @Test
    void drawAllReturnSubsetDrawThatSubset_lastDrawIsConsistent() {
        DeckOfCardsAPIService service = new DeckOfCardsAPIService();
        // create new deck
        String deckId = service.createNewDeckId();
        // shuffle deck 
        service.shuffleDeck(deckId);
        // get all cards
        List<CardDTO> full = service.drawFromDeck(deckId, 52);
        assertEquals(52, full.size());

        // 15 cards to return to the api
        List<Card> toReturn = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Card c = new Card();
            c.setCode(full.get(i).getCode());
            toReturn.add(c);
        }

        // return 15 cards to the api deck
        service.returnDrawnCardsToDeck(deckId, toReturn);
        // shuffle the api deck with 15 cards
        service.shuffleDeck(deckId);
        // draw 15 cards again
        List<CardDTO> redrawn = service.drawFromDeck(deckId, 15);

        assertEquals(15, redrawn.size());
        // codes of 15 redrawn cards
        List<String> again = redrawn.stream().map(CardDTO::getCode).toList();
        // KH / KD still not present (need to be mapped to X1 / X2)
        assertFalse(again.stream().anyMatch(c -> "KH".equals(c) || "KD".equals(c)));
    }

    @Test
    void drawAllReturnAllDrawAllAgain_lastDrawIsConsistent() {
        DeckOfCardsAPIService service = new DeckOfCardsAPIService();
        // create new deck
        String deckId = service.createNewDeckId();
        // shuffle deck
        service.shuffleDeck(deckId);
        // get all cards
        List<CardDTO> first = service.drawFromDeck(deckId, 52);
        assertEquals(52, first.size());

        // prepare all cards to return to api
        List<Card> all = new ArrayList<>();
        for (CardDTO dto : first) {
            Card c = new Card();
            c.setCode(dto.getCode());
            all.add(c);
        }

        // return all cards to api
        service.returnDrawnCardsToDeck(deckId, all);
        // shuffle cards
        service.shuffleDeck(deckId);
        // draw all cards from api again
        List<CardDTO> second = service.drawFromDeck(deckId, 52);

        assertEquals(52, second.size());
        // get all card codes 
        List<String> codes = second.stream().map(CardDTO::getCode).toList();
        // KH and KD are not present
        assertFalse(codes.stream().anyMatch(c -> "KH".equals(c) || "KD".equals(c)));
        // X1 and X2 are present
        assertEquals(1, codes.stream().filter("X1"::equals).count());
        assertEquals(1, codes.stream().filter("X2"::equals).count());
    }
}
