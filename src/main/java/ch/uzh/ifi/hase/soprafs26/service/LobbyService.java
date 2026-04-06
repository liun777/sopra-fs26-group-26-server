package ch.uzh.ifi.hase.soprafs26.service;

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

    public LobbyService(LobbyRepository lobbyRepository,
                        UserRepository userRepository,
                        LobbyEventPublisher lobbyEventPublisher) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.lobbyEventPublisher = lobbyEventPublisher;
    }

    // helper: look up user by token, throw 401 if invalid
    private User getUserByToken(String token) {
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return user;
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
                .anyMatch(l -> "WAITING".equals(l.getStatus()) || "IN_GAME".equals(l.getStatus()));
        if (hasActiveLobby) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active lobby");
        }

        Lobby lobby = new Lobby();
        lobby.setSessionId(generateUniqueSessionId());
        lobby.setSessionHostUserId(host.getId());
        lobby.setIsPublic(isPublic != null ? isPublic : true);
        lobby.getPlayerIds().add(host.getId());

        lobby = lobbyRepository.save(lobby);
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
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
            lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
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
        lobbyEventPublisher.broadcastLobbyUpdate(lobby.getId(), lobby);
        return lobby;
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
}