package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbySettingsPatchDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WaitingLobbyPlayerRowDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WaitingLobbyViewDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final LobbyEventPublisher lobbyEventPublisher;
    private final OnlineUsersEventPublisher onlineUsersEventPublisher;

    public LobbyService(LobbyRepository lobbyRepository,
                        UserRepository userRepository,
                        LobbyEventPublisher lobbyEventPublisher,
                        OnlineUsersEventPublisher onlineUsersEventPublisher) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.lobbyEventPublisher = lobbyEventPublisher;
        this.onlineUsersEventPublisher = onlineUsersEventPublisher;
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

    // generates a unique sessionId
    private String generateUniqueSessionId() {
        String sessionId;
        do {
            sessionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (lobbyRepository.findBySessionId(sessionId) != null);
        return sessionId;
    }

    // POST /lobbies — create a new lobby
    public Lobby createLobby(String token, Boolean isPublic) {
        User host = getUserByToken(token);

        boolean hasActiveLobby = lobbyRepository.findBySessionHostUserId(host.getId()).stream()
                .anyMatch(l -> "WAITING".equals(l.getStatus()) || "PLAYING".equals(l.getStatus()));
        if (hasActiveLobby) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active lobby");
        }

        Lobby lobby = new Lobby();
        lobby.setSessionId(generateUniqueSessionId());
        lobby.setSessionHostUserId(host.getId());
        lobby.setIsPublic(isPublic != null ? isPublic : true);
        lobby.getPlayerIds().add(host.getId());

        lobby = lobbyRepository.save(lobby);
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
        if (!lobby.getPlayerIds().contains(guestUserId)) {
            lobby.getPlayerIds().add(guestUserId);
            lobbyRepository.save(lobby);
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
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found");
        }
        if (!"WAITING".equals(lobby.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid lobby settings update");
        }
        if (!lobby.getPlayerIds().contains(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this lobby");
        }

        Long hostId = lobby.getSessionHostUserId();
        List<Long> orderedIds = new ArrayList<>();
        orderedIds.add(hostId);
        lobby.getPlayerIds().stream()
                .filter(id -> !id.equals(hostId))
                .sorted(Comparator.naturalOrder())
                .forEach(orderedIds::add);

        WaitingLobbyViewDTO dto = new WaitingLobbyViewDTO();
        dto.setLobbyId(lobby.getId());
        dto.setSessionId(lobby.getSessionId());
        dto.setIsPublic(lobby.getIsPublic());
        List<WaitingLobbyPlayerRowDTO> rows = new ArrayList<>();
        for (Long pid : orderedIds) {
            User u = userRepository.findById(pid).orElse(null);
            if (u == null) {
                continue;
            }
            WaitingLobbyPlayerRowDTO row = new WaitingLobbyPlayerRowDTO();
            row.setUsername(u.getUsername());
            row.setJoinStatus(pid.equals(user.getId()) ? "you" : "joined");
            rows.add(row);
        }
        dto.setPlayers(rows);
        return dto;
    }

    public Lobby updateLobbySettings(String token, String sessionId, LobbySettingsPatchDTO body) {
        if (body == null || body.getIsPublic() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No settings to update");
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
        lobby.setIsPublic(body.getIsPublic());
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
        if (lobby.getPlayerIds().size() >= 4) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Player limit is 4!");
        }
        if (lobby.getPlayerIds().contains(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in lobby");
        }

        lobby.getPlayerIds().add(user.getId());
        lobby = lobbyRepository.save(lobby);
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
        setUsersStatus(lobby.getPlayerIds(), UserStatus.PLAYING);
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        onlineUsersEventPublisher.broadcastOnlineUsers();
    }

    // GET /lobbies — get all public lobbies
    public List<Lobby> getPublicLobbies(String token) {
        getUserByToken(token); // just validates the token
        return lobbyRepository.findAll().stream()
                .filter(l -> l.getIsPublic())
                .filter(l -> l.getPlayerIds().size() < 4)
                .filter(l -> l.getStatus().equals("WAITING"))
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

        lobby.getPlayerIds().remove(targetUserId);

        // if no players left — delete the lobby
        if (lobby.getPlayerIds().isEmpty()) {
            lobbyRepository.delete(lobby);
            return null;
        }

        // if the removed player was the host — migrate to next player
        if (lobby.getSessionHostUserId().equals(targetUserId)) {
            lobby.setSessionHostUserId(lobby.getPlayerIds().get(0));
        }

        lobby = lobbyRepository.save(lobby);
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        return lobby;
    }
}
