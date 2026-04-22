package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.config.settings.LobbySettingsProperties;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbySettingsPatchDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WaitingLobbyPlayerRowDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WaitingLobbyViewDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final LobbyEventPublisher lobbyEventPublisher;
    private final OnlineUsersEventPublisher onlineUsersEventPublisher;
    private final DisconnectService disconnectService;
    private final GameService gameService;
    private final LobbySettingsProperties lobbySettings;
    // Players that timed out while being in a PLAYING lobby.
    // They stay part of the active game and trigger an automatic Cabo when their turn starts.
    private final Set<Long> timedOutInPlayingPlayerIds = ConcurrentHashMap.newKeySet();

    public LobbyService(LobbyRepository lobbyRepository,
                        GameRepository gameRepository,
                        UserRepository userRepository,
                        LobbyEventPublisher lobbyEventPublisher,
                        OnlineUsersEventPublisher onlineUsersEventPublisher,
                        LobbySettingsProperties lobbySettings,
                        @Lazy DisconnectService disconnectService,
                        @Lazy GameService gameService) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.lobbyEventPublisher = lobbyEventPublisher;
        this.onlineUsersEventPublisher = onlineUsersEventPublisher;
        this.lobbySettings = lobbySettings;
        this.disconnectService = disconnectService;
        this.gameService = gameService;
    }

    // helper: look up user by token, throw 401 if invalid
    private User getUserByToken(String token) {
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return user;
    }

    private void setUserStatus(Long userId, UserStatus status) {
        if (userId == null || status == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
    }

    // same as setUserStatus but multiple userIds
    private void setUsersStatus(List<Long> userIds, UserStatus status) {
        if (userIds == null || status == null) {
            return;
        }
        for (Long userId : userIds) {
            setUserStatus(userId, status);
        }
    }

    public boolean isPlayerTimedOutInPlaying(Long userId) {
        return userId != null && timedOutInPlayingPlayerIds.contains(userId);
    }

    public void clearTimedOutPlayingFlag(Long userId) {
        if (userId == null) {
            return;
        }
        timedOutInPlayingPlayerIds.remove(userId);
    }

    private void clearTimedOutPlayingFlags(List<Long> userIds) {
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            clearTimedOutPlayingFlag(userId);
        }
    }

    private long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private long normalizeTimerValue(Long rawValue, long min, long max, long defaultValue) {
        long baseValue = rawValue == null ? defaultValue : rawValue;
        return clamp(baseValue, min, max);
    }

    private boolean normalizeLobbySettingsInPlace(Lobby lobby) {
        if (lobby == null) {
            return false;
        }

        boolean changed = false;

        long normalizedAfk = normalizeTimerValue(
                lobby.getAfkTimeoutSeconds(),
                lobbySettings.getAfkTimeoutMinSeconds(),
                lobbySettings.getAfkTimeoutMaxSeconds(),
                lobbySettings.getAfkTimeoutDefaultSeconds());
        if (!Long.valueOf(normalizedAfk).equals(lobby.getAfkTimeoutSeconds())) {
            lobby.setAfkTimeoutSeconds(normalizedAfk);
            changed = true;
        }

        long normalizedInitialPeek = normalizeTimerValue(
                lobby.getInitialPeekSeconds(),
                lobbySettings.getInitialPeekMinSeconds(),
                lobbySettings.getInitialPeekMaxSeconds(),
                lobbySettings.getInitialPeekDefaultSeconds());
        if (!Long.valueOf(normalizedInitialPeek).equals(lobby.getInitialPeekSeconds())) {
            lobby.setInitialPeekSeconds(normalizedInitialPeek);
            changed = true;
        }

        long normalizedTurn = normalizeTimerValue(
                lobby.getTurnSeconds(),
                lobbySettings.getTurnMinSeconds(),
                lobbySettings.getTurnMaxSeconds(),
                lobbySettings.getTurnDefaultSeconds());
        if (!Long.valueOf(normalizedTurn).equals(lobby.getTurnSeconds())) {
            lobby.setTurnSeconds(normalizedTurn);
            changed = true;
        }

        long normalizedAbilityReveal = normalizeTimerValue(
                lobby.getAbilityRevealSeconds(),
                lobbySettings.getAbilityRevealMinSeconds(),
                lobbySettings.getAbilityRevealMaxSeconds(),
                lobbySettings.getAbilityRevealDefaultSeconds());
        if (!Long.valueOf(normalizedAbilityReveal).equals(lobby.getAbilityRevealSeconds())) {
            lobby.setAbilityRevealSeconds(normalizedAbilityReveal);
            changed = true;
        }

        long normalizedRematchDecision = normalizeTimerValue(
                lobby.getRematchDecisionSeconds(),
                lobbySettings.getRematchDecisionMinSeconds(),
                lobbySettings.getRematchDecisionMaxSeconds(),
                lobbySettings.getRematchDecisionDefaultSeconds());
        if (!Long.valueOf(normalizedRematchDecision).equals(lobby.getRematchDecisionSeconds())) {
            lobby.setRematchDecisionSeconds(normalizedRematchDecision);
            changed = true;
        }

        long normalizedWebsocketGrace = normalizeTimerValue(
                lobby.getWebsocketGraceSeconds(),
                lobbySettings.getWebsocketGraceMinSeconds(),
                lobbySettings.getWebsocketGraceMaxSeconds(),
                lobbySettings.getWebsocketGraceDefaultSeconds());
        if (!Long.valueOf(normalizedWebsocketGrace).equals(lobby.getWebsocketGraceSeconds())) {
            lobby.setWebsocketGraceSeconds(normalizedWebsocketGrace);
            changed = true;
        }

        return changed;
    }

    private void applyDefaultTimerSettings(Lobby lobby) {
        if (lobby == null) {
            return;
        }
        lobby.setAfkTimeoutSeconds(lobbySettings.getAfkTimeoutDefaultSeconds());
        lobby.setInitialPeekSeconds(lobbySettings.getInitialPeekDefaultSeconds());
        lobby.setTurnSeconds(lobbySettings.getTurnDefaultSeconds());
        lobby.setAbilityRevealSeconds(lobbySettings.getAbilityRevealDefaultSeconds());
        lobby.setRematchDecisionSeconds(lobbySettings.getRematchDecisionDefaultSeconds());
        lobby.setWebsocketGraceSeconds(lobbySettings.getWebsocketGraceDefaultSeconds());
    }

    // generates a unique sessionId
    private String generateUniqueSessionId() {
        String sessionId;
        do {
            sessionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (lobbyRepository.findBySessionId(sessionId) != null);
        return sessionId;
    }

    public boolean isUserInActiveGame(Long userId) {
        if (userId == null || gameRepository == null) {
            return false;
        }
        return gameRepository.findGamesByPlayerId(userId).stream()
                .filter(game -> game != null && game.getStatus() != GameStatus.ROUND_ENDED)
                .anyMatch(game -> game.getOrderedPlayerIds() != null && game.getOrderedPlayerIds().contains(userId));
    }

    public Set<Long> getPlayingLobbyPlayerIdsSnapshot() {
        Set<Long> playerIds = new LinkedHashSet<>();
        for (Lobby lobby : lobbyRepository.findByStatus("PLAYING")) {
            if (lobby != null && lobby.getPlayerIds() != null) {
                playerIds.addAll(lobby.getPlayerIds());
            }
        }
        return playerIds;
    }

    public Long findWebsocketGraceSecondsForUser(Long userId) {
        if (userId == null) {
            return null;
        }
        Lobby waitingLobby = lobbyRepository.findByStatusAndPlayerId("WAITING", userId).stream()
                .findFirst()
                .orElse(null);
        if (waitingLobby != null && waitingLobby.getWebsocketGraceSeconds() != null && waitingLobby.getWebsocketGraceSeconds() > 0) {
            return waitingLobby.getWebsocketGraceSeconds();
        }
        Lobby playingLobby = lobbyRepository.findByStatusAndPlayerId("PLAYING", userId).stream()
                .findFirst()
                .orElse(null);
        if (playingLobby != null && playingLobby.getWebsocketGraceSeconds() != null && playingLobby.getWebsocketGraceSeconds() > 0) {
            return playingLobby.getWebsocketGraceSeconds();
        }
        return null;
    }

    private void cleanupStalePlayingLobbiesForHost(Long hostUserId) {
        if (hostUserId == null) {
            return;
        }

        // If host is still in an active game, PLAYING lobby is legitimate.
        if (isUserInActiveGame(hostUserId)) {
            return;
        }

        List<Lobby> stalePlayingLobbies = lobbyRepository.findBySessionHostUserId(hostUserId).stream()
                .filter(lobby -> "PLAYING".equals(lobby.getStatus()))
                .toList();
        if (stalePlayingLobbies.isEmpty()) {
            return;
        }

        for (Lobby staleLobby : stalePlayingLobbies) {
            List<Long> playerIds = new ArrayList<>(staleLobby.getPlayerIds());
            lobbyRepository.delete(staleLobby);
            setUsersStatus(playerIds, UserStatus.ONLINE);
        }
        onlineUsersEventPublisher.broadcastOnlineUsers();
    }

    private List<Lobby> findWaitingLobbiesForPlayer(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return lobbyRepository.findByStatusAndPlayerId("WAITING", userId);
    }

    public String findWaitingSessionIdForPlayer(Long userId) {
        return findWaitingLobbiesForPlayer(userId).stream()
                .findFirst()
                .map(Lobby::getSessionId)
                .orElse(null);
    }

    /**
     * Guarantees a player only belongs to one waiting lobby at a time.
     * Keeps `keepSessionId` if provided and removes the player from all others.
     */
    private void leaveOtherWaitingLobbies(Long userId, String keepSessionId) {
        List<Lobby> otherWaitingLobbies = findWaitingLobbiesForPlayer(userId).stream()
                .filter(lobby -> keepSessionId == null || !keepSessionId.equals(lobby.getSessionId()))
                .toList();

        for (Lobby previousLobby : otherWaitingLobbies) {
            removePlayerFromDisconnect(previousLobby.getSessionId(), userId);
        }
    }

    private boolean hasFreshHeartbeat(Long userId, long freshnessSeconds) {
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(User::getLastHeartbeat)
                .map(last -> last != null && last.isAfter(Instant.now().minusSeconds(freshnessSeconds)))
                .orElse(false);
    }

    // POST /lobbies — create a new lobby
    public Lobby createLobby(String token, Boolean isPublic) {
        User host = getUserByToken(token);
        if (isUserInActiveGame(host.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot create a lobby during an active game");
        }
        leaveOtherWaitingLobbies(host.getId(), null);
        cleanupStalePlayingLobbiesForHost(host.getId());
        boolean hostInActiveGame = isUserInActiveGame(host.getId());

        boolean hasActiveLobby = lobbyRepository.findBySessionHostUserId(host.getId()).stream()
                .anyMatch(l -> "WAITING".equals(l.getStatus())
                        || ("PLAYING".equals(l.getStatus()) && hostInActiveGame));
        if (hasActiveLobby) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active lobby");
        }

        Lobby lobby = new Lobby();
        lobby.setSessionId(generateUniqueSessionId());
        lobby.setSessionHostUserId(host.getId());
        lobby.setIsPublic(isPublic != null ? isPublic : true);
        lobby.getPlayerIds().add(host.getId());
        applyDefaultTimerSettings(lobby);

        lobby = lobbyRepository.save(lobby);
        clearTimedOutPlayingFlag(host.getId());
        setUserStatus(host.getId(), UserStatus.LOBBY);  // set host user status to LOBBY after joining lobby
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        onlineUsersEventPublisher.broadcastOnlineUsers();
        return lobby;
    }

    public Optional<Lobby> findWaitingLobbyForHost(Long hostUserId) {
        return lobbyRepository.findBySessionHostUserId(hostUserId).stream()
                .filter(l -> "WAITING".equals(l.getStatus()))
                .findFirst();
    }

    public Lobby requireWaitingLobbyForHost(Long hostUserId) {
        return findWaitingLobbyForHost(hostUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Create a lobby first"));
    }

    public boolean isLobbyWaiting(Long lobbyId) {
        if (lobbyId == null) {
            return false;
        }
        return lobbyRepository.findById(lobbyId)
                .map(l -> "WAITING".equals(l.getStatus()))
                .orElse(false);
    }

    public void addPlayerToLobby(Long lobbyId, Long hostUserId, Long guestUserId) {
        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElse(null);
        if (lobby == null || !hostUserId.equals(lobby.getSessionHostUserId())) {
            return;
        }
        if (!"WAITING".equals(lobby.getStatus())) {
            return;
        }
        if (lobby.getPlayerIds().size() >= 4) {
            return;
        }
        if (isUserInActiveGame(guestUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot join a lobby during an active game");
        }
        if (lobby.getKickedUserIds() != null) {
            lobby.getKickedUserIds().remove(guestUserId);
        }
        if (!lobby.getPlayerIds().contains(guestUserId)) {
            leaveOtherWaitingLobbies(guestUserId, lobby.getSessionId());
            lobby.getPlayerIds().add(guestUserId);
            lobbyRepository.save(lobby);
            clearTimedOutPlayingFlag(guestUserId);
            setUserStatus(guestUserId, UserStatus.LOBBY);  // set user status to LOBBY after joining lobby
            lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
            onlineUsersEventPublisher.broadcastOnlineUsers();
        }
    }

    public String getWaitingSessionIdIfPlayerInLobby(Long lobbyId, Long guestUserId) {
        return lobbyRepository.findById(lobbyId)
                .filter(l -> "WAITING".equals(l.getStatus()) && l.getPlayerIds().contains(guestUserId))
                .map(Lobby::getSessionId)
                .orElse(null);
    }

    public Lobby getMyWaitingLobbyAsHost(String token) {
        User host = getUserByToken(token);
        return lobbyRepository.findBySessionHostUserId(host.getId()).stream()
                .filter(l -> "WAITING".equals(l.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No waiting lobby"));
    }

    public WaitingLobbyViewDTO getWaitingLobbyView(String token, String sessionId) {
        User user = getUserByToken(token);
        Lobby lobby = lobbyRepository.findBySessionId(sessionId);
        if (lobby == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        if (!lobby.getPlayerIds().contains(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not part of this lobby");
        }
        if (normalizeLobbySettingsInPlace(lobby)) {
            lobby = lobbyRepository.save(lobby);
        }
    
        Long hostId = lobby.getSessionHostUserId();
    
        // Sort players so Host is always at the top
        List<Long> orderedIds = new ArrayList<>();
        orderedIds.add(hostId);
        lobby.getPlayerIds().stream()
                .filter(id -> !id.equals(hostId))
                .sorted()
                .forEach(orderedIds::add);

        WaitingLobbyViewDTO dto = new WaitingLobbyViewDTO();
        dto.setLobbyId(lobby.getId());
        dto.setSessionId(lobby.getSessionId());
        dto.setIsPublic(lobby.getIsPublic());
        dto.setAfkTimeoutSeconds(lobby.getAfkTimeoutSeconds());
        dto.setInitialPeekSeconds(lobby.getInitialPeekSeconds());
        dto.setTurnSeconds(lobby.getTurnSeconds());
        dto.setAbilityRevealSeconds(lobby.getAbilityRevealSeconds());
        dto.setRematchDecisionSeconds(lobby.getRematchDecisionSeconds());
        dto.setWebsocketGraceSeconds(lobby.getWebsocketGraceSeconds());
        dto.setViewerIsHost(user.getId().equals(hostId));
    
        List<WaitingLobbyPlayerRowDTO> rows = new ArrayList<>();
        for (Long pid : orderedIds) {
            User u = userRepository.findById(pid).orElse(null);
            if (u == null) continue;

            WaitingLobbyPlayerRowDTO row = new WaitingLobbyPlayerRowDTO();
            row.setUsername(u.getUsername());

            // --- THIS IS THE CRITICAL LOGIC FOR THE START BUTTON ---
            // 1. If this row belongs to the Host, status MUST be "host"
            // 2. If it's NOT the host but it's "me", status is "you"
            // 3. Otherwise, it's just "joined"
            if (pid.equals(hostId)) {
                row.setJoinStatus("host"); 
            } else if (pid.equals(user.getId())) {
                row.setJoinStatus("you");
            } else {
                row.setJoinStatus("joined");
            }
        
            rows.add(row);
        }
        dto.setPlayers(rows);
        return dto;
    }

    public Lobby updateLobbySettings(String token, String sessionId, LobbySettingsPatchDTO body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No settings to update");
        }
        boolean hasAnySetting = body.getIsPublic() != null
                || body.getAfkTimeoutSeconds() != null
                || body.getInitialPeekSeconds() != null
                || body.getTurnSeconds() != null
                || body.getAbilityRevealSeconds() != null
                || body.getWebsocketGraceSeconds() != null;
        if (!hasAnySetting) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No settings to update");
        }
        if (body.getRematchDecisionSeconds() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rematch decision timer is fixed");
        }
        User user = getUserByToken(token);
        Lobby lobby = lobbyRepository.findBySessionId(sessionId);
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found");
        }
        if (!user.getId().equals(lobby.getSessionHostUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the session host can update lobby settings");
        }
        if (!"WAITING".equals(lobby.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid lobby settings update");
        }
        if (body.getIsPublic() != null) {
            lobby.setIsPublic(body.getIsPublic());
        }
        if (body.getAfkTimeoutSeconds() != null) {
            long value = clamp(
                    body.getAfkTimeoutSeconds(),
                    lobbySettings.getAfkTimeoutMinSeconds(),
                    lobbySettings.getAfkTimeoutMaxSeconds());
            lobby.setAfkTimeoutSeconds(value);
        }
        if (body.getInitialPeekSeconds() != null) {
            long value = clamp(
                    body.getInitialPeekSeconds(),
                    lobbySettings.getInitialPeekMinSeconds(),
                    lobbySettings.getInitialPeekMaxSeconds());
            lobby.setInitialPeekSeconds(value);
        }
        if (body.getTurnSeconds() != null) {
            long value = clamp(
                    body.getTurnSeconds(),
                    lobbySettings.getTurnMinSeconds(),
                    lobbySettings.getTurnMaxSeconds());
            lobby.setTurnSeconds(value);
        }
        if (body.getAbilityRevealSeconds() != null) {
            long value = clamp(
                    body.getAbilityRevealSeconds(),
                    lobbySettings.getAbilityRevealMinSeconds(),
                    lobbySettings.getAbilityRevealMaxSeconds());
            lobby.setAbilityRevealSeconds(value);
        }
        if (body.getWebsocketGraceSeconds() != null) {
            long value = clamp(
                    body.getWebsocketGraceSeconds(),
                    lobbySettings.getWebsocketGraceMinSeconds(),
                    lobbySettings.getWebsocketGraceMaxSeconds());
            lobby.setWebsocketGraceSeconds(value);
        }
        normalizeLobbySettingsInPlace(lobby);
        lobby = lobbyRepository.save(lobby);
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        return lobby;
    }

    // POST /lobbies/{sessionId}/players — join a lobby
    public Lobby joinLobby(String sessionId, String token) {
        User user = getUserByToken(token);
        Lobby lobby = lobbyRepository.findBySessionId(sessionId);

        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found");
        }
        if (!"WAITING".equals(lobby.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is not in waiting state");
        }
        if (lobby.getPlayerIds().size() >= 4) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Player limit is 4!");
        }
        if (lobby.getPlayerIds().contains(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in lobby");
        }
        if (isUserInActiveGame(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot join a lobby during an active game");
        }
        if (lobby.getKickedUserIds() != null && lobby.getKickedUserIds().contains(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You were kicked from this lobby");
        }

        leaveOtherWaitingLobbies(user.getId(), sessionId);
        lobby.getPlayerIds().add(user.getId());
        lobby = lobbyRepository.save(lobby);
        clearTimedOutPlayingFlag(user.getId());
        setUserStatus(user.getId(), UserStatus.LOBBY); // set user status to LOBBY after joining lobby
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        onlineUsersEventPublisher.broadcastOnlineUsers();
        return lobby;
    }

    public Lobby verifyLobbyCanStart(String token, String sessionId) {
        User requester = getUserByToken(token);
        Lobby lobby = lobbyRepository.findBySessionId(sessionId);
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found");
        }
        if (!requester.getId().equals(lobby.getSessionHostUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the session host can start the game");
        }
        if (!"WAITING".equals(lobby.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is not in waiting state");
        }

        // Ensure no player is currently in the "60s grace period"
        for (Long pid : lobby.getPlayerIds()) {
            if (disconnectService != null && disconnectService.isPlayerInGracePeriod(pid)) {
                // If the player has a fresh heartbeat, the grace flag is stale (e.g. websocket reconnect race).
                // Clear it and allow start.
                if (hasFreshHeartbeat(pid, 45)) {
                    disconnectService.cancelDisconnectTimer(pid);
                    continue;
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player disconnected");
            }
        }

        return lobby;
    }

    // set players as PLAYING
    public void markLobbyAsPlaying(String sessionId) {
        Lobby lobby = lobbyRepository.findBySessionId(sessionId);
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found");
        }

        lobby.setStatus("PLAYING");
        lobbyRepository.save(lobby);
        clearTimedOutPlayingFlags(lobby.getPlayerIds());
        setUsersStatus(lobby.getPlayerIds(), UserStatus.PLAYING);
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        onlineUsersEventPublisher.broadcastOnlineUsers();
    }

    /**
     * Called when a game round reaches ROUND_ENDED.
     * Transitions the matching PLAYING lobby back to WAITING and updates present players to LOBBY.
     */
    public void handleRoundEndedForGamePlayers(List<Long> gamePlayerIds) {
        handleRoundResolvedForGamePlayers(gamePlayerIds, List.of());
    }

    public void handleRoundResolvedForGamePlayers(List<Long> gamePlayerIds, List<Long> rematchPlayerIds) {
        handleRoundResolvedForGamePlayers(gamePlayerIds, rematchPlayerIds, List.of());
    }

    public void handleRoundResolvedForGamePlayers(
            List<Long> gamePlayerIds,
            List<Long> continueRematchPlayerIds,
            List<Long> freshRematchPlayerIds) {
        if (gamePlayerIds == null || gamePlayerIds.isEmpty()) {
            return;
        }

        List<Lobby> candidates = lobbyRepository.findByStatus("PLAYING").stream()
                .filter(lobby -> lobby.getPlayerIds() != null && !Collections.disjoint(lobby.getPlayerIds(), gamePlayerIds))
                .sorted(Comparator.comparingInt((Lobby lobby) ->
                        (int) lobby.getPlayerIds().stream().filter(gamePlayerIds::contains).count()
                ).reversed())
                .toList();

        if (candidates.isEmpty()) {
            clearTimedOutPlayingFlags(gamePlayerIds);
            setUsersStatus(gamePlayerIds, UserStatus.ONLINE);
            onlineUsersEventPublisher.broadcastOnlineUsers();
            return;
        }

        Lobby currentLobby = candidates.get(0);
        List<Long> orderedGamePlayers = new ArrayList<>(gamePlayerIds);
        clearTimedOutPlayingFlags(orderedGamePlayers);

        Set<Long> continueRematchSet = continueRematchPlayerIds == null
                ? Set.of()
                : new LinkedHashSet<>(continueRematchPlayerIds);
        Set<Long> freshRematchSet = freshRematchPlayerIds == null
                ? Set.of()
                : new LinkedHashSet<>(freshRematchPlayerIds);

        List<Long> normalizedContinuePlayers = orderedGamePlayers.stream()
                .filter(continueRematchSet::contains)
                .toList();
        List<Long> normalizedFreshPlayers = orderedGamePlayers.stream()
                .filter(freshRematchSet::contains)
                .toList();

        if (normalizedContinuePlayers.size() >= 2) {
            currentLobby.setStatus("WAITING");
            currentLobby.setSessionHostUserId(normalizedContinuePlayers.get(0));
            currentLobby.setPlayerIds(new ArrayList<>(normalizedContinuePlayers));
            currentLobby.setKickedUserIds(new ArrayList<>());
            currentLobby = lobbyRepository.save(currentLobby);
            setUsersStatus(normalizedContinuePlayers, UserStatus.LOBBY);
            lobbyEventPublisher.broadcastLobbyUpdate(currentLobby.getId(), currentLobby);
        } else {
            lobbyRepository.delete(currentLobby);
        }

        if (normalizedFreshPlayers.size() >= 2) {
            Lobby freshLobby = new Lobby();
            freshLobby.setSessionId(generateUniqueSessionId());
            freshLobby.setSessionHostUserId(normalizedFreshPlayers.get(0));
            freshLobby.setIsPublic(currentLobby.getIsPublic());
            freshLobby.setStatus("WAITING");
            freshLobby.setPlayerIds(new ArrayList<>(normalizedFreshPlayers));
            freshLobby.setKickedUserIds(new ArrayList<>());
            freshLobby.setAfkTimeoutSeconds(currentLobby.getAfkTimeoutSeconds());
            freshLobby.setInitialPeekSeconds(currentLobby.getInitialPeekSeconds());
            freshLobby.setTurnSeconds(currentLobby.getTurnSeconds());
            freshLobby.setAbilityRevealSeconds(currentLobby.getAbilityRevealSeconds());
            freshLobby.setRematchDecisionSeconds(currentLobby.getRematchDecisionSeconds());
            freshLobby.setWebsocketGraceSeconds(currentLobby.getWebsocketGraceSeconds());
            freshLobby = lobbyRepository.save(freshLobby);
            setUsersStatus(normalizedFreshPlayers, UserStatus.LOBBY);
            lobbyEventPublisher.broadcastLobbyUpdate(freshLobby.getId(), freshLobby);
        }

        Set<Long> allRematchPlayers = new LinkedHashSet<>(normalizedContinuePlayers);
        allRematchPlayers.addAll(normalizedFreshPlayers);
        List<Long> nonRematchPlayers = orderedGamePlayers.stream()
                .filter(playerId -> !allRematchPlayers.contains(playerId))
                .toList();
        setUsersStatus(nonRematchPlayers, UserStatus.ONLINE);
        onlineUsersEventPublisher.broadcastOnlineUsers();
    }

    // GET /lobbies — get all public lobbies
    public List<Lobby> getPublicLobbies(String token) {
        User requester = getUserByToken(token);
        return lobbyRepository.findByIsPublicTrueAndStatus("WAITING").stream()
                .filter(l -> l.getPlayerIds().size() < 4)
                .filter(l -> l.getKickedUserIds() == null || !l.getKickedUserIds().contains(requester.getId()))
                .toList();
    }

    // get lobby by id — used by WebSocketController
    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found!"));
    }

    public Optional<Lobby> findLobbyById(Long lobbyId) {
        if (lobbyId == null) {
            return Optional.empty();
        }
        return lobbyRepository.findById(lobbyId);
    }

    public Lobby getLobbyBySessionId(String sessionId) {
        Lobby lobby = lobbyRepository.findBySessionId(sessionId);
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found!");
        }
        return lobby;
    }

    // remove player from lobby — self leave or host kick
    @Transactional
    public Lobby removePlayerFromLobby(String sessionId, String token, Long targetUserId) {
        User requester = getUserByToken(token);
        Lobby lobby = getLobbyBySessionId(sessionId);

        // only the player themselves or the host can remove a player
        boolean isSelf = requester.getId().equals(targetUserId);
        boolean isHost = requester.getId().equals(lobby.getSessionHostUserId());

        if (!isSelf && !isHost) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden!");
        }

        if (!lobby.getPlayerIds().contains(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not part of this lobby");
        }

        if ("PLAYING".equals(lobby.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Players cannot be removed from an active game");
        }

        lobby.getPlayerIds().remove(targetUserId);
        clearTimedOutPlayingFlag(targetUserId);
        setUserStatus(targetUserId, UserStatus.ONLINE);
        if (isHost && !isSelf) {
            if (lobby.getKickedUserIds() == null) {
                lobby.setKickedUserIds(new ArrayList<>());
            }
            if (!lobby.getKickedUserIds().contains(targetUserId)) {
                lobby.getKickedUserIds().add(targetUserId);
            }
        }

        // if no players left — delete the lobby
        if (lobby.getPlayerIds().isEmpty()) {
            lobbyRepository.delete(lobby);
            onlineUsersEventPublisher.broadcastOnlineUsers();
            return null;
        }

        // if the removed player was the host — migrate to next player
        if (lobby.getSessionHostUserId().equals(targetUserId)) {
            lobby.setSessionHostUserId(lobby.getPlayerIds().get(0));
        }

        lobby = lobbyRepository.save(lobby);
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        onlineUsersEventPublisher.broadcastOnlineUsers();
        return lobby;
    }

    public void handlePermanentDisconnect(Long userId) {
        // Find the lobby the user was in
        Lobby lobby = lobbyRepository.findByStatusAndPlayerId("WAITING", userId).stream()
                .findFirst()
                .orElseGet(() -> lobbyRepository.findByStatusAndPlayerId("PLAYING", userId).stream()
                        .findFirst()
                        .orElse(null));

        if (lobby == null) {
            clearTimedOutPlayingFlag(userId);
            setUserStatus(userId, UserStatus.OFFLINE);
            onlineUsersEventPublisher.broadcastOnlineUsers();
            return;
        }

        if ("WAITING".equals(lobby.getStatus())) {
            // Lobby timeout: remove from waiting lobby and mark offline.
            clearTimedOutPlayingFlag(userId);
            setUserStatus(userId, UserStatus.OFFLINE);
            this.removePlayerFromDisconnect(lobby.getSessionId(), userId);
        } else if ("PLAYING".equals(lobby.getStatus())) {
            // Midgame timeout: preserve game membership and mark player as timed out.
            // Cabo is auto-called when this player's turn starts.
            timedOutInPlayingPlayerIds.add(userId);
            onlineUsersEventPublisher.broadcastOnlineUsers();
        } else {
            clearTimedOutPlayingFlag(userId);
            setUserStatus(userId, UserStatus.OFFLINE);
            onlineUsersEventPublisher.broadcastOnlineUsers();
        }
    }

    public void removePlayerFromDisconnect(String sessionId, Long userId) {
        Lobby lobby = getLobbyBySessionId(sessionId);
        if ("PLAYING".equals(lobby.getStatus())) {
            timedOutInPlayingPlayerIds.add(userId);
            return;
        }
        lobby.getPlayerIds().remove(userId);
        clearTimedOutPlayingFlag(userId);

        if (lobby.getPlayerIds().isEmpty()) {
            lobbyRepository.delete(lobby);
        } else {
            if (lobby.getSessionHostUserId().equals(userId)) {
                lobby.setSessionHostUserId(lobby.getPlayerIds().get(0));
            }
            lobbyRepository.save(lobby);
            lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        }
        onlineUsersEventPublisher.broadcastOnlineUsers();
    }
}
