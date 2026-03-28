package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

        Lobby lobby = new Lobby();
        lobby.setSessionId(generateUniqueSessionId());
        lobby.setSessionHostUserId(host.getId());
        lobby.setIsPublic(isPublic != null ? isPublic : true);
        lobby.getPlayerIds().add(host.getId());

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

    public Lobby getLobbyBySessionId(String sessionId) {
        Lobby lobby = lobbyRepository.findBySessionId(sessionId);
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found!");
        }
        return lobby;
    }
}