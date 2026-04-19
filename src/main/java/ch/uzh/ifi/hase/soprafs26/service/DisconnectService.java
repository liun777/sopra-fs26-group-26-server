package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class DisconnectService {

    private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final LobbyEventPublisher lobbyEventPublisher;
    private final GameService gameService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public DisconnectService(UserRepository userRepository,
                             LobbyRepository lobbyRepository,
                             GameRepository gameRepository,
                             LobbyEventPublisher lobbyEventPublisher,
                             GameService gameService) {
        this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.lobbyEventPublisher = lobbyEventPublisher;
        this.gameService = gameService;
    }

    public void handleDisconnect(String token) {
        if (token == null || token.isBlank()) return;

        User user = userRepository.findByToken(token);
        if (user == null) return;

        Long userId = user.getId();

        // set user offline and invalidate token
        user.setStatus(UserStatus.OFFLINE);
        user.setToken(UUID.randomUUID().toString());
        userRepository.save(user);

        // handle lobby disconnect
        handleLobbyDisconnect(userId);

        // handle game disconnect
        handleGameDisconnect(userId);
    }

    // remove player from lobby, migrate host if needed
    private void handleLobbyDisconnect(Long userId) {
        List<Lobby> lobbies = lobbyRepository.findAll().stream()
                .filter(l -> "WAITING".equals(l.getStatus()))
                .filter(l -> l.getPlayerIds().contains(userId))
                .toList();

        for (Lobby lobby : lobbies) {
            lobby.getPlayerIds().remove(userId);

            if (lobby.getPlayerIds().isEmpty()) {
                lobbyRepository.delete(lobby);
                continue;
            }

            if (lobby.getSessionHostUserId().equals(userId)) {
                Long newHost = lobby.getPlayerIds().get(0);
                lobby.setSessionHostUserId(newHost);
            }

            lobbyRepository.save(lobby);
            lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        }
    }

    // handle game disconnect — start 60 sec timer then force Cabo
    private void handleGameDisconnect(Long userId) {
        List<Game> games = gameRepository.findGamesByPlayerId(userId).stream()
                .filter(g -> g.getStatus() != GameStatus.ROUND_ENDED)
                .toList();

        for (Game game : games) {
            String gameId = game.getId();
            scheduler.schedule(() -> {
                try {
                    Game currentGame = gameService.getGameById(gameId);
                    if (currentGame.getStatus() == GameStatus.ROUND_ENDED) return;
                    if (userId.equals(currentGame.getCurrentPlayerId())) {
                        currentGame.setCaboCalled(true);
                        currentGame.setCaboCalledByUserId(userId);
                        gameService.advanceTurnToNextPlayer(gameId);
                    }
                } catch (Exception e) {
                    System.err.println("Game disconnect handling failed: " + e.getMessage());
                }
            }, 60, TimeUnit.SECONDS);
        }
    }

    // runs every 30 seconds to check for stale heartbeats
    @Scheduled(fixedDelay = 30000)
    public void checkStaleHeartbeats() {
        Instant cutoff = Instant.now().minusSeconds(60);
        List<User> users = userRepository.findAll();
        for (User user : users) {
            // only check online users that have a heartbeat
            if (user.getStatus() == UserStatus.OFFLINE) continue;
            if (user.getLastHeartbeat() == null) continue;
            if (user.getLastHeartbeat().isBefore(cutoff)) {
                handleDisconnect(user.getToken());
            }
        }
    }
}