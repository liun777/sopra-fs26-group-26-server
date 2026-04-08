package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;


import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;

import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final DeckOfCardsAPIService deckOfCardsAPIService;
    private final UserRepository userRepository;
    private final GameEventPublisher gameEventPublisher;
    // clock that runs tasks in the background
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    // map to store tasks - key: gameId, value: scheduled task
    private final Map<String, ScheduledFuture<?>> gameTimers = new ConcurrentHashMap<>();

    // constructor injection
    public GameService(GameRepository gameRepository, DeckOfCardsAPIService deckOfCardsAPIService,
                       UserRepository userRepository, GameEventPublisher gameEventPublisher) {
        this.gameRepository = gameRepository;
        this.deckOfCardsAPIService = deckOfCardsAPIService;
        this.userRepository = userRepository;
        this.gameEventPublisher = gameEventPublisher;
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
        newGame.setOrderedPlayerIds(new ArrayList<>(playerIds));
        newGame.setCurrentPlayerId(playerIds.get(0));

        startTurnTimer(newGame.getId(), newGame.getCurrentPlayerId());

        // consolidate save and broadcast in one place
        // so every time a game is saved we don't forget to broadcast
        return saveGameAndBroadcast(newGame);
    }

    // helper method that shuffles the discard pile, called when the draw pile is empty
    private void reshuffleDiscardPile(String gameId) {
        Game game = getGameById(gameId);
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

    // Backlog #9: Implement logic to always render the DiscardPile top card with its face-up value
    public Game getGameById(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Game not found"));
    }


    public Card getDiscardPileTopCard(String gameId) {
        Game game = getGameById(gameId);
        List<Card> discardPile = game.getDiscardPile();

        if (discardPile.isEmpty()) {
            return null;
        }


        Card topCard = discardPile.get(discardPile.size() - 1);
        topCard.setVisibility(true);
        return topCard;
    }

    //# 8: Implement a global isMyTurn state that disables all buttons and click listeners on the game board when false.
    // this method says if it is the Users turn or not
    public boolean isMyTurn(String gameId, Long userId) {
        Game game = getGameById(gameId);
        return userId.equals(game.getCurrentPlayerId());
    }

    // Add a "Current Player" check to all incoming move requests; return a 403 Forbidden if it's not their turn. #30
    public void verifyMoveCallerIsCurrentPlayer(String gameId, String authorizationToken) {
        if (authorizationToken == null || authorizationToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        User user = userRepository.findByToken(authorizationToken);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        Game game = getGameById(gameId);
        if (!user.getId().equals(game.getCurrentPlayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn");
        }
    }

    // to save and broadcast: saveGameAndBroadcast(game)
    public void moveDrawFromDrawPile(String gameId) {
        Game game = getGameById(gameId);

        // block drawing during initial peek phase
        if (game.getStatus() != GameStatus.ROUND_ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot draw a card right now");
        }

        // trigger reshuffle if draw pile is empty
        if (game.getDrawPile().isEmpty()) {
            reshuffleDiscardPile(gameId);
            game = getGameById(gameId); // reload game after reshuffle
        }       

        // draw the top card from the draw pile
        Card drawnCard = game.getDrawPile().remove(0);
        drawnCard.setVisibility(true);

        saveGameAndBroadcast(game);
    }

    // the equivalent to moveDrawFromDrawPile - takes the current drawn card and places it on the 
    // discard pile
    public void moveCardToDiscardPile(String gameId) {
        Game game = getGameById(gameId);
        Card drawnCard = game.getDrawnCard();
        List<Card> discardPile = game.getDiscardPile();

        if (drawnCard != null) {
            drawnCard.setVisibility(true);
            discardPile.add(drawnCard);
            game.setDrawnCard(null);
            advanceTurnToNextPlayer(gameId);
        }
    }

    // to save and broadcast: saveGameAndBroadcast(game)
    public void moveCallCabo(String gameId) {
    }


    // save in db and send filtered representations to all players 
    private Game saveGameAndBroadcast(Game game) {
        Game saved = gameRepository.save(game);
        gameRepository.flush();
        gameEventPublisher.publishFilteredState(saved);
        return saved;
    }

    // this is used to automatically end a players turn by drawing and instantly discarding a card 
    // if they are AFK
    public void executeTimoutMove(String gameId, Long userId) {
        Game game = getGameById(gameId);
        Card cardToDiscard = game.getDrawnCard();
        
        if (!userId.equals(game.getCurrentPlayerId())) {
            return;
        }

        if (cardToDiscard == null) {
            moveDrawFromDrawPile(gameId);
        }

        moveCardToDiscardPile(gameId);
    }

    // pass the turn to the next player
    public void advanceTurnToNextPlayer(String gameId) {
        Game game = getGameById(gameId);
        List<Long> players = game.getOrderedPlayerIds();
        Long currentPlayerId = game.getCurrentPlayerId();

        int currentIndex = players.indexOf(currentPlayerId);
        int nextIndex = (currentIndex+1)%players.size();

        game.setCurrentPlayerId(players.get(nextIndex));
        startTurnTimer(gameId, game.getCurrentPlayerId());
        saveGameAndBroadcast(game);
    }

    // start new 30 sec alarm for specified player
    private void startTurnTimer(String gameId, Long playerId) {
        // always cancel running timers first
        cancelTurnTimer(gameId);
        // tell the alarm what to run and when to run it
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                // this runs when the 30 sec are over
                executeTimoutMove(gameId, playerId);
            } catch (Exception e) {
                // catch errors such that bug doesnt permanently crash timer
                System.err.println("Timeout execution failed for game " + gameId + ": " + e.getMessage());
            }
        }, 30, TimeUnit.SECONDS);
        // save it to our tasks
        gameTimers.put(gameId, future);
    }

    // cancels current alarm if player makes a move
    private void cancelTurnTimer(String gameId) {
        ScheduledFuture<?> future = gameTimers.get(gameId);
        if (future != null) {
            // cancel timer
            future.cancel(false);
            gameTimers.remove(gameId);
        }
    }

}
