package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Move;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.MoveRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MoveLogEntryDTO;

@Service
public class MoveService {

    private final MoveRepository moveRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public MoveService(MoveRepository moveRepository,
                       SessionRepository sessionRepository,
                       UserRepository userRepository) {
        this.moveRepository = moveRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    // #111 requester's own moves + other public moves
    public List<MoveLogEntryDTO> getSessionLog(String sessionId, String token) {
        User requester = userRepository.findByToken(token);
        // reject unauthorized request
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Session session = sessionRepository.findBySessionId(sessionId);
        // if no session found - throw exception 
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found");
        }

        // is requester is not part of this session - throw exception
        Long requesterId = requester.getId();
        if (!session.getTotalScoreByUserId().containsKey(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant of this session");
        }

        // order by timestamp ascending
        List<Move> chronologicalMoves = moveRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        // create a list of all public moves and a set of their user ids
        List<Move> visibleMoves = new ArrayList<>();
        Set<Long> playerIds = new HashSet<>();
        for (Move move : chronologicalMoves) {
            // skip invalid moves
            if (move == null) {
                continue;
            }
            boolean isOwnMove = requesterId.equals(move.getUserId());
            boolean isPublicMove = Boolean.TRUE.equals(move.getIsPublic());
            if (isOwnMove || isPublicMove) {
                visibleMoves.add(move);
                playerIds.add(move.getUserId());
            }
        }

        // create a map with user id - username pairs
        Map<Long, String> usernamesByUserId = new HashMap<>();
        if (!playerIds.isEmpty()) {
            for (User player : userRepository.findAllById(playerIds)) {
                if (player != null) {
                    usernamesByUserId.put(player.getId(), player.getUsername());
                }
            }
        }

        // create a log list
        List<MoveLogEntryDTO> log = new ArrayList<>();
        for (Move move : visibleMoves) {
            MoveLogEntryDTO row = new MoveLogEntryDTO();
            row.setUserId(move.getUserId());
            row.setUsername(usernamesByUserId.get(move.getUserId()));
            row.setActionType(move.getActionType());
            row.setTimestamp(move.getTimestamp());
            row.setDetails(move.getDetails());
            row.setOwnMove(requesterId.equals(move.getUserId()));
            log.add(row);
        }
        return log;
    }
}
