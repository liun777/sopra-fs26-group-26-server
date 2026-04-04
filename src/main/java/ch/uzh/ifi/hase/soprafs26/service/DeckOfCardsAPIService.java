package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.DeckResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;

import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

import java.util.List;  


@Service
public class DeckOfCardsAPIService {

    // specify the url of the deck of cards api
    private final String BASE_URL = "https://deckofcardsapi.com/api/deck";
    // initialize a rest template instance
    private final RestTemplate restTemplate;
    // specify which cards we want to use
    private final String CABO_DECK = """
                                        AS,AD,AC,AH,
                                        2S,2D,2C,2H,
                                        3S,3D,3C,3H,
                                        4S,4D,4C,4H,
                                        5S,5D,5C,5H,
                                        6S,6D,6C,6H,
                                        7S,7D,7C,7H,
                                        8S,8D,8C,8H,
                                        9S,9D,9C,9H,
                                        0S,0D,0C,0H,
                                        JS,JD,JC,JH,
                                        QS,QD,QC,QH,
                                        KS,KC,X1,X2
                                        """.replace("\n", "").replace(" ", "");

    public DeckOfCardsAPIService() {
        this.restTemplate = new RestTemplate();
    }

    // get a brand new shuffled cabo deck with 52 cards
    public List<CardDTO> getNewCaboDeck() {
        return createAndDrawDeck(CABO_DECK, 52);
    }

    // shuffle the discard pile
    public List<CardDTO> shuffleCaboDeck(List<String> discardedCards) {
        // convert the list of strings into a long string separated by commas
        String PARTIAL_DECK = String.join(",", discardedCards);
        // call helper method to convert this into a shuffled deck
        return createAndDrawDeck(PARTIAL_DECK, discardedCards.size());
    }

    // helper method that handles all api calls: get a deck with cards specified in the string and shuffle it
    private List<CardDTO> createAndDrawDeck(String cardCodes, int amountToDraw) {
        // url where api shuffles cards specified in the string
        String SHUFFLE_URL = BASE_URL + "/new/shuffle/?cards=" + cardCodes;
        // make get request to get the deck id of the shuffled deck
        String deckId = restTemplate.getForObject(SHUFFLE_URL, DeckResponseDTO.class).getDeckId();
        // draw the specified amount of cards from the deck that was shuffled before
        String CARDS_URL = BASE_URL + "/" + deckId + "/draw/?count=" + amountToDraw;
        // make a get request to obtain the actual cards
        DeckResponseDTO response = restTemplate.getForObject(CARDS_URL, DeckResponseDTO.class);
        return response.getCards();
    }

} 