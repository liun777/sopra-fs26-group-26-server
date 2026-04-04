package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;


@Service
public class GameService {

    private final GameRepository gameRepository;
    private final DeckOfCardsAPIService deckOfCardsAPIService;

    // constructor injection
    public GameService(GameRepository gameRepository, DeckOfCardsAPIService deckOfCardsAPIService) {
        this.gameRepository = gameRepository;
        this.deckOfCardsAPIService = deckOfCardsAPIService;
    }

    public Game startGame(List<Long> playerIds) {
        // create a new game
        Game newGame = new Game();
        // get a deck of cards from the api
        List<CardDTO> apiCards = deckOfCardsAPIService.getNewCaboDeck();
        // convert it into our card entities
        List<Card> drawPile = DTOMapper.INSTANCE.convertCardDTOListtoEntityList(apiCards);
        // assign it to the draw pile
        newGame.setDrawPile(drawPile);
        // initialize the player hands
        Map<Long, List<Card>> playerHands = new HashMap<>();

        // give each player an empty hand
        for (Long id:playerIds) {
            playerHands.put(id, new ArrayList<>());
        }

        // do four rounds of dealing each player one card from the draw pile
        for (int i=0; i<4; i++) {
            for (Long id:playerIds) {
                Card card = drawPile.remove(0);
                playerHands.get(id).add(card);
            }
        }

        // create a discard pile, get one card from the draw pile and place it face up on the 
        // discard pile
        List<Card> discardPile = new ArrayList<>();
        Card firstCard = drawPile.remove(0);
        firstCard.setVisibility(true);
        discardPile.add(firstCard);

        // update all piles and player hands
        newGame.setPlayerHands(playerHands);
        newGame.setDiscardPile(discardPile);
        newGame.setDrawPile(drawPile);
        

        // save it to the DB
        Game savedGame = gameRepository.save(newGame);
        gameRepository.flush();
        return savedGame;
    }

    // helper method that shuffles the discard pile, called when the draw pile is empty
    private void reshuffleDiscardPile(Game game) {
        List<Card> discardPile = game.getDiscardPile();
        // leave the last card in the discard pile
        Card topCard = discardPile.remove(0);
        List<Card> remainingCards = new ArrayList<>();
        remainingCards.add(topCard);
        // this converts the card entities into a list of strings containing the codes of the cards 
        // since the method we want to call expects such a list as input
        List<String> unshuffledCards = discardPile.stream().map(Card::getCode).toList();
        // call the method that lets the api shuffle our deck
        List<CardDTO> shuffledCardDTOs = deckOfCardsAPIService.shuffleCaboDeck(unshuffledCards);
        // convert the api response back into our card entities
        List<Card> shuffledCards = DTOMapper.INSTANCE.convertCardDTOListtoEntityList(shuffledCardDTOs);
        // update the piles
        game.setDrawPile(shuffledCards);
        game.setDiscardPile(remainingCards);
    }
}