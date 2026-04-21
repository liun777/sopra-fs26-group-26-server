package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;


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
import org.springframework.http.HttpStatusCode;
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
    private final ScheduledExecutorService scheduler;
    // map to store tasks - key: gameId, value: scheduled task
    private final Map<String, ScheduledFuture<?>> gameTimers = new ConcurrentHashMap<>();
    // per-game count so an outdated scheduled ability timer cannot execute 
    // already existing cancel mechanism alone is not always enough if a task was queued/running
    // AtomicLong: enables thread-safe increments
    private final Map<String, AtomicLong> abilityTimerCounts = new ConcurrentHashMap<>();

    // constructor injection
    public GameService(GameRepository gameRepository, DeckOfCardsAPIService deckOfCardsAPIService,
                       UserRepository userRepository, GameEventPublisher gameEventPublisher,
                       ScheduledExecutorService scheduler) {
        this.gameRepository = gameRepository;
        this.deckOfCardsAPIService = deckOfCardsAPIService;
        this.userRepository = userRepository;
        this.gameEventPublisher = gameEventPublisher;
        this.scheduler = scheduler;
    }

    public Game startGame(List<Long> playerIds) {
        List<Long> sanitizedPlayerIds = sanitizePlayerIds(playerIds);
        validatePlayerCount(sanitizedPlayerIds);

        // create a new game
        Game newGame = new Game();
        buildInitialDrawPile(newGame);
        List<Card> drawPile = newGame.getDrawPile();
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
        newGame.setStatus(GameStatus.INITIAL_PEEK);
        // Save first to get a generated game id, then start the timer.
        Game saved = saveGameAndBroadcast(newGame);
        startPeekingTimer(saved.getId());

        // startTurnTimer(saved.getId(), saved.getCurrentPlayerId());
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
    private void buildInitialDrawPile(Game newGame) {
        try {
            // create new deck at the api and get its id
            String deckId = deckOfCardsAPIService.createNewDeckId();
            // shuffle the api deck
            deckOfCardsAPIService.shuffleDeck(deckId);
            // draw all cards from the api deck 
            List<CardDTO> apiCards = deckOfCardsAPIService.drawFromDeck(deckId, 52);
            if (apiCards == null || apiCards.isEmpty()) {
                throw new IllegalStateException("Deck API draw returned no cards");
            }
            // convert DTO representation to Entity representation for cards
            List<Card> converted = DTOMapper.INSTANCE.convertCardDTOListtoEntityList(apiCards);
            if (converted == null || converted.isEmpty()) {
                throw new IllegalStateException("Card conversion produced empty list");
            }
            // set the deckId and the DrawPile
            newGame.setDeckApiId(deckId);
            newGame.setDrawPile(new ArrayList<>(converted));
        } catch (Exception ex) {
            System.err.println("Deck API unavailable for startGame; using fallback deck: " + ex.getMessage());
            // if api didnt work and we fallback, DeckApiId is null
            newGame.setDeckApiId(null);
            newGame.setDrawPile(buildFallbackDeck());
        }
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
        if (discardPile.isEmpty()) {
            return;
        }
        // Keep the top card (same logic as in getDiscardPileTopCard) in the discard pile
        // put the rest into draw pile

        // index of top card from discard pile
        int topIdx = discardPile.size() - 1;
        // remove top card from discard pile
        Card topCard = discardPile.remove(topIdx);

        // use the rest of the discard pile to put into draw pile
        List<Card> toPutIntoDrawPile = new ArrayList<>(discardPile);

        // empty discard pile and put only the top card there
        discardPile.clear();
        discardPile.add(topCard);

        // edge case: nothing to put into draw pile
        // set empty draw pile and save
        if (toPutIntoDrawPile.isEmpty()) {
            game.setDrawPile(new ArrayList<>());
            saveGameAndBroadcast(game);
            return;
        }

        // get the deck id from the game
        String deckId = game.getDeckApiId();
        try {
            if (deckId != null) {
                // return cards to the deck at the api
                deckOfCardsAPIService.returnDrawnCardsToDeck(deckId, toPutIntoDrawPile);
                // shuffle the deck
                deckOfCardsAPIService.shuffleDeck(deckId);
                // draw from the deck
                List<CardDTO> dtos = deckOfCardsAPIService.drawFromDeck(deckId, toPutIntoDrawPile.size());
                // set the draw pile, converting from CardDTO to Card Entity representation
                game.setDrawPile(new ArrayList<>(DTOMapper.INSTANCE.convertCardDTOListtoEntityList(dtos)));
            } else {
                // if deck id is null (setting the deckId has initially failed) and we have been using a fallback deck
                Collections.shuffle(toPutIntoDrawPile);
                game.setDrawPile(toPutIntoDrawPile);
            }
        } catch (Exception ex) {
            // if there was an error while talking to the api - fallback to Java's shuffle 
            System.err.println("Deck API reshuffle failed; using Java's shuffle: " + ex.getMessage());
            Collections.shuffle(toPutIntoDrawPile);
            game.setDrawPile(toPutIntoDrawPile);
        }
        saveGameAndBroadcast(game);
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

    // get the player's own hand
    public List<Card> getMyHand(String gameId, String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        Game game = getGameById(gameId);
        List<Card> hand = game.getPlayerHands().get(user.getId());
        if (hand == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a player in this game");
        }
        return hand;
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
            applySpecialPeek(game, authenticatedUser, body);
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

    // 7/8 (own card) or 9/10 (opponent card): reveal one card and broadcast; phase ends when the ability timer fires (same idea as initial peek + global timer).
    private void applySpecialPeek(Game game, User authenticatedUser, PeekSelectionDTO body) {
        Long currentId = game.getCurrentPlayerId();
        if (currentId == null || !authenticatedUser.getId().equals(currentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn");
        }
        GameStatus status = game.getStatus();
        if (status != GameStatus.ABILITY_PEEK_SELF && status != GameStatus.ABILITY_PEEK_OPPONENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No special peek ability active");
        }

        // check if peek was already used
        if (game.isSpecialPeekUsed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Peek ability already used");
        }

        List<Integer> indices = body.getIndices();
        if (indices == null || indices.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly one card index required");
        }
        Integer idx = indices.get(0);
        if (idx == null || idx < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid index");
        }

        Long handOwnerId;
        if (status == GameStatus.ABILITY_PEEK_SELF) {
            Long handUserId = body.getHandUserId();
            if (handUserId != null && !handUserId.equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only peek your own hand");
            }
            handOwnerId = currentId;
        } else {
            Long handUserId = body.getHandUserId();
            if (handUserId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "handUserId required for opponent peek");
            }
            if (handUserId.equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot peek your own hand in opponent peek");
            }
            List<Long> ordered = game.getOrderedPlayerIds();
            if (ordered == null || !ordered.contains(handUserId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target player is not in this game");
            }
            handOwnerId = handUserId;
        }

        Map<Long, List<Card>> hands = game.getPlayerHands();
        List<Card> hand = hands == null ? null : hands.get(handOwnerId);
        if (hand == null || idx >= hand.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Index out of range");
        }

        for (Card c : hand) {
            if (c != null) {
                c.setVisibility(false);
            }
        }
        hand.get(idx).setVisibility(true);

        // mark peek as used
        game.setSpecialPeekUsed(true);

        saveGameAndBroadcast(game);
    }

    private void clearAllHandVisibility(Game game) {
        List<Long> players = game.getOrderedPlayerIds();
        if (players == null) {
            return;
        }
        Map<Long, List<Card>> hands = game.getPlayerHands();
        if (hands == null) {
            return;
        }
        for (Long id : players) {
            List<Card> hand = hands.get(id);
            if (hand != null) {
                for (Card c : hand) {
                    if (c != null) {
                        c.setVisibility(false);
                    }
                }
            }
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
        game.setDrawnCard(drawnCard);
        game.setDrawnFromDeck(true); // mark that this card came from the deck

        saveGameAndBroadcast(game);
    }
    
    // allows to pick a card from the discard pile and safe it into the field drawn card
    public void moveDrawFromDiscardPile(String gameId, String token) {
        verifyMoveCallerIsCurrentPlayer(gameId, token);
        Game game = getGameById(gameId);
        List<Card> discardPile = game.getDiscardPile();
        if (!game.getStatus().equals(GameStatus.ROUND_ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Round is not active.");
        }
        if (game.getDrawnCard() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already drawn a card!");
        }
        if (discardPile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Discard pile is empty.");
        }
        Card drawnCard = discardPile.remove(discardPile.size()-1);
        game.setDrawnCard(drawnCard);

        saveGameAndBroadcast(game);
    }

    // the equivalent to moveDrawFromDrawPile - takes the current drawn card and places it on the 
    // discard pile
    public void moveCardToDiscardPile(String gameId) {
        Game game = getGameById(gameId);
        Card drawnCard = game.getDrawnCard();
        List<Card> discardPile = game.getDiscardPile();

        if (drawnCard != null) {
            boolean wasDrawnFromDeck = game.isDrawnFromDeck(); // save before reset
            drawnCard.setVisibility(true);
            discardPile.add(drawnCard);
            game.setDrawnCard(null);
            game.setDrawnFromDeck(false); // reset flag

            // only trigger ability if card came from draw pile
            if (wasDrawnFromDeck) {
                triggerAbilityIfApplicable(game, drawnCard);
            } else {
                saveGameAndBroadcast(game);
                advanceTurnToNextPlayer(gameId);
            }
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
        game.setDrawnFromDeck(false); // reset flag after swap
        saveGameAndBroadcast(game);
        advanceTurnToNextPlayer(gameId);
    }

    // check card value and trigger ability phase if applicable
    private void triggerAbilityIfApplicable(Game game, Card discardedCard) {
        int value = discardedCard.getValue();
        if (value == 7 || value == 8) {
            // peek at own card
            game.setStatus(GameStatus.ABILITY_PEEK_SELF);
            saveGameAndBroadcast(game);
            // start 30 sec timer to auto-end ability phase
            startAbilityTimer(game.getId());
        } else if (value == 9 || value == 10) {
            // peek at opponent's card
            game.setStatus(GameStatus.ABILITY_PEEK_OPPONENT);
            saveGameAndBroadcast(game);
            startAbilityTimer(game.getId());
        } else if (value == 11 || value == 12) {
            // swap cards with opponent
            game.setStatus(GameStatus.ABILITY_SWAP);
            saveGameAndBroadcast(game);
            startAbilityTimer(game.getId());
        } else {
            // no ability — just advance turn normally
            saveGameAndBroadcast(game);
            advanceTurnToNextPlayer(game.getId());
        }
    }

    // auto-end ability phase after 30 seconds if player doesn't act
    private void startAbilityTimer(String gameId) {
        cancelTurnTimer(gameId);
        long scheduledCount = abilityTimerCounts
                .computeIfAbsent(gameId, ignored -> new AtomicLong(0)) // if there is no count for this game id, set to 0
                .incrementAndGet(); // increment by 1 and retrieve
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                Game game = getGameById(gameId);
                if (isCurrentAbilityTimerCount(gameId, scheduledCount)) {
                    completeAbilityPhaseAndAdvance(gameId, game, scheduledCount);
                }
            } catch (Exception e) {
                System.err.println("Ability timer failed for game " + gameId + ": " + e.getMessage());
            }
        }, 30, TimeUnit.SECONDS);
        gameTimers.put(gameId, future);
    }

    // True if scheduledCount is still the latest count for this game's ability timers
    private boolean isCurrentAbilityTimerCount(String gameId, long scheduledCount) {
        AtomicLong latest = abilityTimerCounts.get(gameId);
        return latest != null && latest.get() == scheduledCount;
    }

    private boolean isAbilityPhase(GameStatus status) {
        return status == GameStatus.ABILITY_PEEK_SELF
                || status == GameStatus.ABILITY_PEEK_OPPONENT
                || status == GameStatus.ABILITY_SWAP;
    }


     // expectedCountIfAny is non-null only from startAbilityTimer method, where it must match latest count
     // null from other paths (e.g. away from keyboard turn timeout) — no count check
    private boolean completeAbilityPhaseAndAdvance(String gameId, Game game, Long expectedCountIfAny) {
        GameStatus status = game.getStatus();
        if (!isAbilityPhase(status)) {
            return false;
        }
        if (expectedCountIfAny != null && !isCurrentAbilityTimerCount(gameId, expectedCountIfAny)) {
            return false;
        }

        if (status == GameStatus.ABILITY_PEEK_SELF || status == GameStatus.ABILITY_PEEK_OPPONENT) {
            clearAllHandVisibility(game);
        }

        game.setSpecialPeekUsed(false); // reset peek flag

        game.setStatus(GameStatus.ROUND_ACTIVE);
        cancelTurnTimer(gameId);
        saveGameAndBroadcast(game);
        advanceTurnToNextPlayer(gameId);
        return true;
    }

    // swap one card from current player's hand with a card from opponent's hand
    public void moveAbilitySwap(String gameId, String token, int ownCardIndex, Long targetUserId, int targetCardIndex) {
        // verify it's the player's turn
        verifyMoveCallerIsCurrentPlayer(gameId, token);

        Game game = getGameById(gameId);

        // verify game is in ability swap phase
        if (game.getStatus() != GameStatus.ABILITY_SWAP) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Swap ability is not available");
        }

        // get both hands
        Long currentPlayerId = game.getCurrentPlayerId();
        List<Card> ownHand = game.getPlayerHands().get(currentPlayerId);
        List<Card> targetHand = game.getPlayerHands().get(targetUserId);

        // validate target player exists
        if (targetHand == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target player not found");
        }

        // cannot swap with yourself
        if (currentPlayerId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot swap with yourself");
        }

        // validate indices
        if (ownCardIndex < 0 || ownCardIndex >= ownHand.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card index");
        }
        if (targetCardIndex < 0 || targetCardIndex >= targetHand.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target card index");
        }

        // swap the cards — neither card's visibility changes
        Card ownCard = ownHand.remove(ownCardIndex);
        Card targetCard = targetHand.remove(targetCardIndex);

        ownCard.setVisibility(false);
        targetCard.setVisibility(false);

        ownHand.add(ownCardIndex, targetCard);
        targetHand.add(targetCardIndex, ownCard);

        // end ability phase, go back to next player's turn
        game.setStatus(GameStatus.ROUND_ACTIVE);
        // cancel pending timer, player finished the ability manually
        cancelTurnTimer(gameId);
        saveGameAndBroadcast(game);
        advanceTurnToNextPlayer(gameId);
    }

    // swap top card of discard pile with one of the player's hand cards
    public void moveSwapWithDiscardPile(String gameId, String token, int targetCardIndex) {
    // guard 1: verify it's the player's turn
    verifyMoveCallerIsCurrentPlayer(gameId, token);

    Game game = getGameById(gameId);

    // guard 2: player must not have already drawn a card from the draw pile
    if (game.getDrawnCard() != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot swap with discard pile after drawing a card");
    }

    // guard 3: discard pile must not be empty
    List<Card> discardPile = game.getDiscardPile();
    if (discardPile.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Discard pile is empty");
    }

    // get the player's hand
    Long currentPlayerId = game.getCurrentPlayerId();
    List<Card> playerHand = game.getPlayerHands().get(currentPlayerId);

    // validate the target index
    if (targetCardIndex < 0 || targetCardIndex >= playerHand.size()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card index");
    }

    // take the top card from the discard pile
    Card topDiscardCard = discardPile.remove(discardPile.size() - 1);

    // remove the player's selected card from their hand
    Card replacedCard = playerHand.remove(targetCardIndex);

    // put the discard pile card into the player's hand face-down
    topDiscardCard.setVisibility(false);
    playerHand.add(targetCardIndex, topDiscardCard);

    // put the replaced card face-up on the discard pile
    replacedCard.setVisibility(true);
    discardPile.add(replacedCard);

    // advance turn
    saveGameAndBroadcast(game);
    advanceTurnToNextPlayer(gameId);
    }
    
    // handles callindgcabo - assumes that cabo is in itself a turn and no card can be drawn if a player wants to call cabo
    public void moveCallCabo(String gameId, String token) {
        verifyMoveCallerIsCurrentPlayer(gameId, token);
        Game game = getGameById(gameId);

        if (game.getStatus() != GameStatus.ROUND_ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot call Cabo right now");
        }

        if (game.isCaboCalled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cabo has already been called");
        }
        // a player can either call cabo OR draw a card
        if (game.getDrawnCard() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot call Cabo after drawing a card");
        }

        game.setCaboCalled(true);
        game.setCaboCalledByUserId(game.getCurrentPlayerId());
        saveGameAndBroadcast(game);
        advanceTurnToNextPlayer(gameId); 
    }

    // Internal method for system-triggered Cabo (no token required)
    public void forceCallCabo(String gameId, Long userId) {
        Game game = getGameById(gameId);
    
        // Safety checks
        if (game.getStatus() == GameStatus.ROUND_ACTIVE && !game.isCaboCalled()) {
            game.setCaboCalled(true);
            game.setCaboCalledByUserId(userId);
            saveGameAndBroadcast(game);
            advanceTurnToNextPlayer(gameId);
        }
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
        GameStatus status = game.getStatus();
        Card cardToDiscard = game.getDrawnCard();
        List<Card> discardPile = game.getDiscardPile();
        
        if (!userId.equals(game.getCurrentPlayerId())) {
            return;
        }

        if(status == GameStatus.ROUND_ACTIVE) {

            // make sure a card is drawn - if not, draw one from the draw pile (triggering reshuffle if necessary)
            if (cardToDiscard == null) {
                if (game.getDrawPile().isEmpty()) {
                    reshuffleDiscardPile(gameId);
                    game = getGameById(gameId);
                }
                cardToDiscard = game.getDrawPile().remove(0);
            }
            // discard the card (it is visible since it is being discarded)
            cardToDiscard.setVisibility(true);
            discardPile.add(cardToDiscard);
            game.setDrawnCard(null);
            game.setDrawnFromDeck(false);
        }

        if (isAbilityPhase(status)) {
            // null: no ability timer count to validate (this path is from the turn timer, not startAbilityTimer)
            completeAbilityPhaseAndAdvance(gameId, game, null);
            return;
        }
        // save changes and advance turn
        saveGameAndBroadcast(game);
        advanceTurnToNextPlayer(gameId);
    }

    // pass the turn to the next player
    public void advanceTurnToNextPlayer(String gameId) {
        Game game = getGameById(gameId);
        List<Long> players = game.getOrderedPlayerIds();
        Long currentPlayerId = game.getCurrentPlayerId();

        int currentIndex = players.indexOf(currentPlayerId);
        int nextIndex = (currentIndex+1)%players.size();
        Long nextPlayerId = players.get(nextIndex);

        // makes sure the game only advances one round after cabo is called
        if (game.isCaboCalled() && nextPlayerId.equals(game.getCaboCalledByUserId())) {
            game.setStatus(GameStatus.ROUND_ENDED);
            saveGameAndBroadcast(game);
            return;
        }

        game.setCurrentPlayerId(nextPlayerId);
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

    private void startPeekingTimer(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            return;
        }
        // make sure no other timer runs
        cancelTurnTimer(gameId);
        // start timer that allows players to do intial peek
        ScheduledFuture<?> future = scheduler.schedule( () -> {
            endPeekingTimer(gameId);
        }, 10, TimeUnit.SECONDS);
        gameTimers.put(gameId, future);
    }

    private void endPeekingTimer(String gameId) {
        Game game = getGameById(gameId);
        List<Integer> randomIndices = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Map<Long, Boolean> performedInitialPeek = game.getInitialPeekDoneByUserId();
        List<Long> players = game.getOrderedPlayerIds();

        if (performedInitialPeek == null) {
            performedInitialPeek = new HashMap<>();
            game.setInitialPeekDoneByUserId(performedInitialPeek);
        }

        // iterate through players to make sure all of them made initial peek
        for (Long id : players) {
            if (!Boolean.TRUE.equals(performedInitialPeek.get(id))) {
                // if a player didnt do their initial peek select two random cards and reveal them
                Collections.shuffle(randomIndices);
                // select the two random cards
                int firstIndex = randomIndices.get(0);
                int secondIndex = randomIndices.get(1);
                List<Card> hand = game.getPlayerHands().get(id);
                // reveal them
                hand.get(firstIndex).setVisibility(true);
                hand.get(secondIndex).setVisibility(true);
                // state that the players did their initial peek
                performedInitialPeek.put(id, true);
            }
        }

        // Bevor die Runde startet, alle Karten für alle Spieler wieder auf unsichtbar setzen
        clearAllHandVisibility(game);

        // randomly select who starts with the first move
        int randomStarterIndex = new java.util.Random().nextInt(players.size());
        Long starterId = players.get(randomStarterIndex);
        // set that player as the first one to move 
        game.setCurrentPlayerId(starterId);
        // set game status
        game.setStatus(GameStatus.ROUND_ACTIVE);
        // broadcast changes
        saveGameAndBroadcast(game);
        // initialize timer for turns
        startTurnTimer(gameId, starterId);

    }
    // #20 drawn card only reveals value to the right player
    public Card getDrawnCard(String gameId, String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Game game = getGameById(gameId);

        // Nur aktueller Spieler darf Karte sehen
        if (!user.getId().equals(game.getCurrentPlayerId())) {
            return null;
        }

        return game.getDrawnCard();
    }

    public void skipAbility(String gameId, String token) {
        verifyMoveCallerIsCurrentPlayer(gameId, token);
        Game game = getGameById(gameId);
        GameStatus status = game.getStatus();
        if (status != GameStatus.ABILITY_PEEK_SELF && status != GameStatus.ABILITY_PEEK_OPPONENT && status != GameStatus.ABILITY_SWAP) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No ability to skip");
        }
        if (status == GameStatus.ABILITY_PEEK_SELF || status == GameStatus.ABILITY_PEEK_OPPONENT) {
            clearAllHandVisibility(game);
        }

        game.setSpecialPeekUsed(false); // reset peek flag
        
        game.setStatus(GameStatus.ROUND_ACTIVE);
        // cancel pending timer, player finished the ability manually
        cancelTurnTimer(gameId);
        saveGameAndBroadcast(game);
        advanceTurnToNextPlayer(gameId);
    }
}   
