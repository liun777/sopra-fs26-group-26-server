package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.config.settings.TimeoutSettingsProperties;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Transactional
public class DisconnectService {

    private final UserRepository userRepository;
    private final LobbyService lobbyService;
    private final TimeoutSettingsProperties timeoutSettings;

    private final Map<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    // Tracks currently active websocket sessions per user so we only start the 60s timer
    // when the last session disconnects.
    private final Map<Long, Set<String>> activeWebSocketSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public DisconnectService(UserRepository userRepository,
                             @Lazy LobbyService lobbyService,
                             TimeoutSettingsProperties timeoutSettings) {
        this.userRepository = userRepository;
        this.lobbyService = lobbyService;
        this.timeoutSettings = timeoutSettings;
    }

    /**
     * RULE 1: Connection Loss (WebSocket closed/Tab closed).
     * Starts the websocket grace period.
     */
    public void handleConnectionLoss(Long userId) {
        if (userId == null) return;
        if (hasActiveWebSocketSession(userId)) {
            return;
        }
        cancelDisconnectTimer(userId);
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            performPermanentRemoval(userId);
        }, timeoutSettings.getWebsocketGraceSeconds(), TimeUnit.SECONDS);
        
        activeTimers.put(userId, future);
    }

    /**
     * RULE 2: Idle Timeout (User is there but silent for a longer period).
     * Checked every minute via @Scheduled.
     */
    @Scheduled(fixedDelayString = "#{@timeoutSettingsProperties.idleCheckIntervalMs}")
    public void checkIdleUsers() {
        Instant idleCutoff = Instant.now().minusSeconds(timeoutSettings.getIdleSeconds());
        
        List<User> activeUsers = userRepository.findAll().stream()
            .filter(u -> u.getStatus() != UserStatus.OFFLINE)
            .filter(u -> u.getLastHeartbeat() != null && u.getLastHeartbeat().isBefore(idleCutoff))
            .toList();

        for (User user : activeUsers) {
            // If they are idle long enough, remove them from waiting/online presence.
            performPermanentRemoval(user.getId());
        }
    }

    /**
     * RULE 3: Automatic logout (token invalidation) for very long inactivity.
     * This is intentionally much longer than idle disconnect timers.
     */
    @Scheduled(fixedDelayString = "#{@timeoutSettingsProperties.autoLogoutCheckIntervalMs}")
    public void checkAutoLogoutUsers() {
        Instant autoLogoutCutoff = Instant.now().minusSeconds(timeoutSettings.getAutoLogoutSeconds());

        List<User> usersToAutoLogout = userRepository.findAll().stream()
                .filter(user -> user.getLastHeartbeat() != null && user.getLastHeartbeat().isBefore(autoLogoutCutoff))
                // Never invalidate tokens while a user is still in an active game.
                .filter(user -> user.getStatus() != UserStatus.PLAYING)
                .toList();

        for (User user : usersToAutoLogout) {
            user.setStatus(UserStatus.OFFLINE);
            user.setToken(UUID.randomUUID().toString());
            userRepository.save(user);

            activeTimers.remove(user.getId());
            activeWebSocketSessions.remove(user.getId());
        }
    }

    public void handleReconnect(Long userId) {
        cancelDisconnectTimer(userId);
        lobbyService.clearTimedOutPlayingFlag(userId);
    }

    public void registerWebSocketSession(Long userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }

        activeWebSocketSessions
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        cancelDisconnectTimer(userId);
        lobbyService.clearTimedOutPlayingFlag(userId);
    }

    public void unregisterWebSocketSession(Long userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }

        Set<String> sessions = activeWebSocketSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                activeWebSocketSessions.remove(userId);
            }
        }

        if (!hasActiveWebSocketSession(userId)) {
            handleConnectionLoss(userId);
        }
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

    private boolean hasActiveWebSocketSession(Long userId) {
        Set<String> sessions = activeWebSocketSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Final cleanup after websocket grace or idle timeout.
     */
    private void performPermanentRemoval(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // Delegate lobby/game-aware timeout handling.
        lobbyService.handlePermanentDisconnect(userId);
        
        activeTimers.remove(userId);
        activeWebSocketSessions.remove(userId);
        System.out.println("User " + userId + " permanently removed due to timeout.");
    }
}
