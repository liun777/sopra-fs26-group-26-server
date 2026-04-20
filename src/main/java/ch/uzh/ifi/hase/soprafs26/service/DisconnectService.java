package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Transactional
public class DisconnectService {

    private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final LobbyEventPublisher lobbyEventPublisher;
    private final GameService gameService;
    private final LobbyService lobbyService; // Added to use your restored LobbyService methods
    
    private final Map<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public DisconnectService(UserRepository userRepository,
                             LobbyRepository lobbyRepository,
                             GameRepository gameRepository,
                             LobbyEventPublisher lobbyEventPublisher,
                             @Lazy GameService gameService,
                             @Lazy LobbyService lobbyService) {
        this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.lobbyEventPublisher = lobbyEventPublisher;
        this.gameService = gameService;
        this.lobbyService = lobbyService;
    }

    /**
     * RULE 1: Connection Loss (WebSocket closed/Tab closed).
     * Starts the 60-second grace period.
     */
    public void handleConnectionLoss(Long userId) {
        if (userId == null) return;
        cancelDisconnectTimer(userId);
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            performPermanentRemoval(userId);
        }, 60, TimeUnit.SECONDS);
        
        activeTimers.put(userId, future);
    }

    /**
     * RULE 2: Idle Timeout (User is there but silent for 5 minutes).
     * Checked every minute via @Scheduled.
     */
    @Scheduled(fixedDelay = 60000)
    public void checkIdleUsers() {
        // 300s = 5 minutes of no activity (no moves, no pings)
        Instant idleCutoff = Instant.now().minusSeconds(300);
        
        List<User> activeUsers = userRepository.findAll().stream()
            .filter(u -> u.getStatus() != UserStatus.OFFLINE)
            .filter(u -> u.getLastHeartbeat() != null && u.getLastHeartbeat().isBefore(idleCutoff))
            .toList();

        for (User user : activeUsers) {
            // If they are idle for 5 mins, we remove them immediately
            performPermanentRemoval(user.getId());
        }
    }

    public void handleReconnect(Long userId) {
        cancelDisconnectTimer(userId);
    }

    public void cancelDisconnectTimer(Long userId) {
        ScheduledFuture<?> future = activeTimers.remove(userId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public boolean isPlayerInGracePeriod(Long userId) {
        return activeTimers.containsKey(userId);
    }

    /**
     * Final Cleanup: Called when either the 60s or 5min timer expires.
     */
    private void performPermanentRemoval(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // Invalidate session
        user.setStatus(UserStatus.OFFLINE);
        user.setToken(UUID.randomUUID().toString());
        userRepository.save(user);

        // Delegate to your restored LobbyService methods
        lobbyService.handlePermanentDisconnect(userId);
        
        activeTimers.remove(userId);
        System.out.println("User " + userId + " permanently removed due to timeout.");
    }
}