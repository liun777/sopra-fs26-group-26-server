package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Arrays;


import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PeekSelectionDTO;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.util.PeekType;

import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// added TEMPORARY FALLBACK, SINCE DECKAPI IS UNRELIABLE FOR TESTING
@Service
public class GameService {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;
    private static final int STARTER_CARDS_PER_PLAYER = 4;
    private static final List<String> FALLBACK_CABO_CARD_CODES = Arrays.asList(
            "AS", "AD", "AC", "AH",
            "2S", "2D", "2C", "2H",
            "3S", "3D", "3C", "3H",
            "4S", "4D", "4C", "4H",
            "5S", "5D", "5C", "5H",
            "6S", "6D", "6C", "6H",
            "7S", "7D", "7C", "7H",
            "8S", "8D", "8C", "8H",
            "9S", "9D", "9C", "9H",
            "0S", "0D", "0C", "0H",
            "JS", "JD", "JC", "JH",
            "QS", "QD", "QC", "QH",
            "KS", "KC", "X1", "X2"
    );

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
        List<Long> sanitizedPlayerIds = sanitizePlayerIds(playerIds);
        validatePlayerCount(sanitizedPlayerIds);

        // create a new game
        Game newGame = new Game();
        // TEMP??? get card deck from api, use fallback to local deck if api unavailable
        List<Card> drawPile = buildInitialDrawPile();
        // assign it to the draw pile
        newGame.setDrawPile(drawPile);
        // initialize the player hands
        Map<Long, List<Card>> playerHands = new HashMap<>();

        // give each player an empty hand
        for (Long id:sanitizedPlayerIds) {
            playerHands.put(id, new ArrayList<>());
        }

        int cardsNeeded = (STARTER_CARDS_PER_PLAYER * sanitizedPlayerIds.size()) + 1;
        if (drawPile.size() < cardsNeeded) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not initialize deck");
        }

        // do four rounds of dealing each player one card from the draw pile
        for (int i=0; i<STARTER_CARDS_PER_PLAYER; i++) {
            for (Long id:sanitizedPlayerIds) {
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
        newGame.setOrderedPlayerIds(new ArrayList<>(sanitizedPlayerIds));
        newGame.setCurrentPlayerId(sanitizedPlayerIds.get(0));

        // Save first to get a generated game id, then start the timer.
        Game saved = saveGameAndBroadcast(newGame);
        startTurnTimer(saved.getId(), saved.getCurrentPlayerId());
        return saved;
    }

    private List<Long> sanitizePlayerIds(List<Long> playerIds) {
        if (playerIds == null) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long playerId : playerIds) {
            if (playerId != null) {
                unique.add(playerId);
            }
        }
        return new ArrayList<>(unique);
    }
    
    // EXCEPTION FOR PLAYER AMOUNT REQUIREMENTS
    private void validatePlayerCount(List<Long> playerIds) {
        if (playerIds.size() < MIN_PLAYERS || playerIds.size() > MAX_PLAYERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby requires 2 to 4 players");
        }
    }

    // ALSO HAS TEMP??? FALLBACK IN IT for building deck
    private List<Card> buildInitialDrawPile() {
        try {
            List<CardDTO> apiCards = deckOfCardsAPIService.getNewCaboDeck();
            if (apiCards != null && !apiCards.isEmpty()) {
                List<Card> converted = DTOMapper.INSTANCE.convertCardDTOListtoEntityList(apiCards);
                if (converted != null && !converted.isEmpty()) {
                    return new ArrayList<>(converted);
                }
            }
        } catch (Exception ex) {
            System.err.println("Deck API unavailable for startGame; using fallback deck: " + ex.getMessage());
        }
        return buildFallbackDeck();
    }

    // TEMP??? FALLBACK for building deck
    private List<Card> buildFallbackDeck() {
        List<Card> fallback = new ArrayList<>();
        for (String code : FALLBACK_CABO_CARD_CODES) {
            Card card = new Card();
            card.setCode(code);
            card.setVisibility(false);
            card.setValue(mapCardCodeToValue(code));
            fallback.add(card);
        }
        Collections.shuffle(fallback);
        return fallback;
    }

    private int mapCardCodeToValue(String code) {
        if (code == null || code.isBlank()) {
            return 0;
        }
        char firstChar = code.charAt(0);
        return switch (firstChar) {
            case 'X' -> 0;
            case 'A' -> 1;
            case '0' -> 10;
            case 'J' -> 11;
            case 'Q' -> 12;
            case 'K' -> 13;
            default -> Character.getNumericValue(firstChar);
        };
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


     // #47 initial peek + per-user broadcast 
     // #49 authentication guards.
    public void applyPeek(String gameId, String token, PeekSelectionDTO body) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        User authenticatedUser = userRepository.findByToken(token);
        if (authenticatedUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        if (body == null || body.getPeekType() == null || body.getPeekType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "peekType is required");
        }
        // Locale.ROOT ensures consistent character manipulation regardless of server's language settings 
        String peekType = body.getPeekType().trim().toLowerCase(Locale.ROOT);
        if (!PeekType.INITIAL.equals(peekType) && !PeekType.SPECIAL.equals(peekType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "peekType must be \"initial\" or \"special\"");
        }

        Game game = getGameById(gameId);

        if (PeekType.SPECIAL.equals(peekType)) {
            // implement
            applySpecialPeek();
            return;
        }

        applyInitialPeek(game, authenticatedUser, body);
    }

    private void applyInitialPeek(Game game, User authenticatedUser, PeekSelectionDTO body) {
        Long authenticatedUserId = authenticatedUser.getId();
        Long handUserId = body.getHandUserId();
        if (handUserId != null && !handUserId.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot peek another player's hand");
        }

        List<Integer> indices = body.getIndices();
        if (indices == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indices required");
        }
        if (indices.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly two card indices required");
        }
        Integer a = indices.get(0);
        Integer b = indices.get(1);
        if (a == null || b == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indices cannot be null");
        }
        if (a.equals(b)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indices must be distinct");
        }

        if (game.getStatus() != GameStatus.INITIAL_PEEK) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not in initial peek phase");
        }

        Map<Long, Boolean> performedInitialPeek = game.getInitialPeekDoneByUserId();
        if (performedInitialPeek == null) {
            performedInitialPeek = new HashMap<>();
            game.setInitialPeekDoneByUserId(performedInitialPeek);
        }
        // use Boolean.TRUE cause .get() may return a null
        if (Boolean.TRUE.equals(performedInitialPeek.get(authenticatedUserId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Initial peek already used");
        }

        List<Card> hand = game.getPlayerHands().get(authenticatedUserId);
        if (hand == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a player in this game");
        }
        if (a < 0 || a >= hand.size() || b < 0 || b >= hand.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Index out of range");
        }

        // reset visibility, then reveal two selected cards
        for (Card c : hand) {
            if (c != null) {
                c.setVisibility(false);
            }
        }
        hand.get(a).setVisibility(true);
        hand.get(b).setVisibility(true);

        performedInitialPeek.put(authenticatedUserId, true);
        saveGameAndBroadcast(game);
    }

    // future work
    private void applySpecialPeek() {

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

    // swap drawn card with one of the player's hand cards
    public void moveSwapDrawnCard(String gameId, String token, int targetCardIndex) {
        // verify it's the player's turn
        verifyMoveCallerIsCurrentPlayer(gameId, token);

        Game game = getGameById(gameId);
        Card drawnCard = game.getDrawnCard();

        // check there is actually a drawn card
        if (drawnCard == null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "No drawn card available for swapping");
        }

        // get the current player's hand
        Long currentPlayerId = game.getCurrentPlayerId();
        List<Card> playerHand = game.getPlayerHands().get(currentPlayerId);

        // validate the target index
        if (targetCardIndex < 0 || targetCardIndex >= playerHand.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card index");
        }

        // remove the card at the target index from the hand
        Card replacedCard = playerHand.remove(targetCardIndex);

        // put the drawn card in its place, face-down
        drawnCard.setVisibility(false);
        playerHand.add(targetCardIndex, drawnCard);

        // put the replaced card face-up on the discard pile
        replacedCard.setVisibility(true);
        game.getDiscardPile().add(replacedCard);

        // clear the drawn card and advance turn
        game.setDrawnCard(null);
        saveGameAndBroadcast(game);
        advanceTurnToNextPlayer(gameId);
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
        if (gameId == null || gameId.isBlank() || playerId == null) {
            return;
        }
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
        if (gameId == null || gameId.isBlank()) {
            return;
        }
        ScheduledFuture<?> future = gameTimers.get(gameId);
        if (future != null) {
            // cancel timer
            future.cancel(false);
            gameTimers.remove(gameId);
        }
    }

}
