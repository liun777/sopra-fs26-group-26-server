package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.time.LocalDate;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */


// user service ist sozusagen brain vom backend - enthält die gnaze logik:
    // COntroller empfängt requests und gibt es dem service und der service macht dann die ganze arbeit
@Service
@Transactional
public class UserService {
    private static final long HEARTBEAT_WRITE_THROTTLE_SECONDS = 10;

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;
    private final SessionRepository sessionRepository;
	private final OnlineUsersEventPublisher onlineUsersEventPublisher;
    private final DisconnectService disconnectService;

	public UserService(@Qualifier("userRepository") UserRepository userRepository,
                       @Qualifier("lobbyRepository") LobbyRepository lobbyRepository,
                       @Lazy SessionRepository sessionRepository,
	                   OnlineUsersEventPublisher onlineUsersEventPublisher,
                       @Lazy DisconnectService disconnectService) {
		this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
        this.sessionRepository = sessionRepository;
		this.onlineUsersEventPublisher = onlineUsersEventPublisher;
		this.disconnectService = disconnectService;
	}

    private boolean isUserInPlayingLobby(Long userId) {
        if (userId == null || lobbyRepository == null) {
            return false;
        }
        return lobbyRepository.existsByStatusAndPlayerId("PLAYING", userId);
    }

    // holt alle user aus Datenbank und gibt sie dem controller
	public List<User> getUsers() {
        List<User> users = this.userRepository.findAll();
        recalculateGlobalRankingFromSessions(users);
		return users;
	}

    // #102: global ranking 
    // gamesWon, averageScorePerSession - based on totalScore from ended sessions
    // roundsWon, averageScorePerRound - from all rounds of all sessions (ended + ongoing)
    private void recalculateGlobalRankingFromSessions(List<User> users) {
        if (users == null || users.isEmpty() || sessionRepository == null) {
            return;
        }

        Map<Long, Integer> sessionWinsByUserId = new HashMap<>();
        Map<Long, Long> cumulativeSessionScoreByUserId = new HashMap<>();
        Map<Long, Integer> playedSessionsByUserId = new HashMap<>();

        Map<Long, Integer> roundWinsByUserId = new HashMap<>();
        Map<Long, Long> cumulativeRoundScoreByUserId = new HashMap<>();
        Map<Long, Integer> roundsPlayedByUserId = new HashMap<>();

        for (Session session : sessionRepository.findAll()) {
            // skip invalid sessions
            if (session == null) {
                continue;
            }

            // get round scores for session
            List<Map<Long, Integer>> perRound = session.getUserScoresPerRound();

            // skip invalid per round scores
            if (perRound != null) {
                // get the "user id - score" map for each round of current session
                for (Map<Long, Integer> roundMap : perRound) {
                    // skip invalid round score maps
                    if (roundMap == null || roundMap.isEmpty()) {
                        continue;
                    }
                    // get winning score for the round
                    Integer bestRoundScore = null;
                    // iterate all scores of current round
                    for (Integer s : roundMap.values()) {
                        // skip invalid scores
                        if (s == null) {
                            continue;
                        }
                        // update best score for the round
                        if (bestRoundScore == null || s < bestRoundScore) {
                            bestRoundScore = s;
                        }
                    }
                    // if nothing was written to the best round score - skip
                    if (bestRoundScore == null) {
                        continue;
                    }
                    // go through "user id - score" pairs of round scores map
                    for (Map.Entry<Long, Integer> entry : roundMap.entrySet()) {
                        Long userId = entry.getKey();
                        Integer score = entry.getValue();
                        // if user id or score invalid - skip
                        if (userId == null || score == null) {
                            continue;
                        }
                        // update aggregated round metrics
                        cumulativeRoundScoreByUserId.merge(userId, score.longValue(), Long::sum);
                        roundsPlayedByUserId.merge(userId, 1, Integer::sum);
                        // increment the round wins only when score matches best score  (or for all users that tie)
                        if (Objects.equals(score, bestRoundScore)) {
                            roundWinsByUserId.merge(userId, 1, Integer::sum);
                        }
                    }
                }
            }

            // we now proceed to session level metrics - games won, average score per session
            // these are calculated only over ended sessions - otherwise skip
            if (!session.isEnded()) {
                continue;
            }

            // get total score map (user id - total score) for current session
            Map<Long, Integer> totalScoreByUserId = session.getTotalScoreByUserId();
            // if total score map is invalid - skip
            if (totalScoreByUserId == null || totalScoreByUserId.isEmpty()) {
                continue;
            }

            // get best total score for current session
            Integer bestSessionScore = totalScoreByUserId.values().stream()
                    .filter(Objects::nonNull) // leave out nulls 
                    .min(Integer::compareTo) // get smallest total score
                    .orElse(null); // if nothing is left after filtering - return null
            // if best session score is invalid - skip
            if (bestSessionScore == null) {
                continue;
            }
            // iterate all "user id - totalScore" pairs for current session
            for (Map.Entry<Long, Integer> entry : totalScoreByUserId.entrySet()) {
                Long userId = entry.getKey();
                Integer score = entry.getValue();
                // if "user id - totalScore" pair invalid - skip
                if (userId == null || score == null) {
                    continue;
                }
                // update aggregated session metrics
                cumulativeSessionScoreByUserId.merge(userId, score.longValue(), Long::sum);
                playedSessionsByUserId.merge(userId, 1, Integer::sum);
                // increment the session wins for best scoring user only (or all users that tie)
                if (Objects.equals(score, bestSessionScore)) {
                    sessionWinsByUserId.merge(userId, 1, Integer::sum);
                }
            }
        }

        
        List<User> ordered = new ArrayList<>(users);
        // per user, calculate average scores (round and session based)
        // get win values (round and session based) from temporary maps
        for (User user : ordered) {
            Long userId = user.getId();
            // if no values were added to the maps: return 0
            int sessionWins = sessionWinsByUserId.getOrDefault(userId, 0);
            int playedSessions = playedSessionsByUserId.getOrDefault(userId, 0);
            long cumulativeSession = cumulativeSessionScoreByUserId.getOrDefault(userId, 0L);
            // if no sessions were played - set 0. otherwise calculate average
            int averageSession = playedSessions == 0 ? 0
                    : (int) Math.round((double) cumulativeSession / playedSessions);

            // if no values were added to the maps: return 0
            int roundsPlayed = roundsPlayedByUserId.getOrDefault(userId, 0);
            long cumulativeRound = cumulativeRoundScoreByUserId.getOrDefault(userId, 0L);
            // if no rounds were played - set 0. otherwise calculate average
            int averageRound = roundsPlayed == 0 ? 0
                    : (int) Math.round((double) cumulativeRound / roundsPlayed);

            // set average scores and win values to user objects
            user.setGamesWon(sessionWins);
            user.setRoundsWon(roundWinsByUserId.getOrDefault(userId, 0));
            user.setAverageScorePerSession(averageSession);
            user.setAverageScorePerRound(averageRound);
        }

        // order users
        // first by games won, descending
        // then by average score over sessions, ascending
        // then by ids, ascending
        ordered.sort(Comparator
                .comparing(User::getGamesWon, Comparator.nullsFirst(Comparator.reverseOrder()))
                .thenComparing(User::getAverageScorePerSession, Comparator.nullsFirst(Integer::compareTo))
                .thenComparing(User::getId, Comparator.nullsFirst(Long::compareTo)));

        // calculate and assign ranks to users, based on the above sort order
        int rank = 1;
        for (User user : ordered) {
            user.setOverallRank(rank++);
            userRepository.save(user);
        }
        userRepository.flush();
    }

	public User createUser(User newUser) {
        if (newUser.getUsername() != null && newUser.getUsername().length() > 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be 16 characters or fewer.");
        }
		newUser.setToken(UUID.randomUUID().toString()); // generiert einen zufälligen eindeutigen Token
		newUser.setStatus(UserStatus.ONLINE); // neuer User ist sofort ONLINE
        newUser.setCreationDate(LocalDate.now()); // setzt heutiges datum
		checkIfUserExists(newUser); // schaut ob dese user beriets exisiter
		// saves the given entity but data is only persisted in the database once
        // speichert in datenbank
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		onlineUsersEventPublisher.broadcastOnlineUsers();
		return newUser;
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	//private void checkIfUserExists(User userToBeCreated) {
		//User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		//User userByName = userRepository.findByName(userToBeCreated.getName());

		//String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
		//if (userByUsername != null && userByName != null) {
		//	throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				//	String.format(baseErrorMessage, "username and the name", "are"));
		//} else if (userByUsername != null) {
		//	throw new ResponseStatusException(HttpStatus.CONFLICT, "This username is already taken, chose a new one");
		//} else if (userByName != null) {
		//	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
		//}
	//}

    // name prüfung auskommentiert von mir weil sie nicht mehr gebraucht wird, sondern nur check via username
    // schauen ob username bereits exisitier, wenn ja conflict error
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

        if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
    }




    // sucht user anhand von ID falls nicht gefunden not found error
    public User getUserById(Long userId) { return userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
// änderung des PW in datenbank
    public void updateUser(Long userId, User userInput) { User user = getUserById(userId);
        if (userInput.getPassword() != null) {
            user.setPassword(userInput.getPassword()); // nur updaten wenn neues passwort vorhanden
        }
        if (userInput.getStatus() != null) {
            user.setStatus(userInput.getStatus());
        }
        userRepository.save(user);
        userRepository.flush();
        onlineUsersEventPublisher.broadcastOnlineUsers();
    }
// schauen ob username bereits existier und ob PW korrekt ist, wenn nicht unauthorized fehler, falls
    // alles gut dann wird stauts auf online gesetzet
    public User loginUser(String username, String password) {User user = userRepository.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
// stimmt pw?
        if (!user.getPassword().equals(password)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

// falls ja status ändern zu online
        user.setStatus(resolveStatusForLogin(user.getId()));
        userRepository.save(user);
        userRepository.flush();
        onlineUsersEventPublisher.broadcastOnlineUsers();
        return user;
    }

    private UserStatus resolveStatusForLogin(Long userId) {
        if (userId == null || lobbyRepository == null) {
            return UserStatus.ONLINE;
        }

        boolean inPlayingLobby = lobbyRepository.existsByStatusAndPlayerId("PLAYING", userId);
        if (inPlayingLobby) {
            return UserStatus.PLAYING;
        }

        boolean inWaitingLobby = lobbyRepository.existsByStatusAndPlayerId("WAITING", userId);
        if (inWaitingLobby) {
            return UserStatus.LOBBY;
        }

        return UserStatus.ONLINE;
    }

	// logout needs to be authenticated according to REST interface
	public void logoutUser(String token) {
		User foundUser = userRepository.findByToken(token);

		if(foundUser == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");
		}

        if (isUserInPlayingLobby(foundUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot logout during an active game");
        }

		foundUser.setStatus(UserStatus.OFFLINE);
		// this saves a random token to the user but the token is never revealed and no-one can use it
		// acts as a safety feature s.t. a user that is logged out has no valid token saved in the DB
		foundUser.setToken(UUID.randomUUID().toString());
		userRepository.save(foundUser);
		userRepository.flush();
        if (disconnectService != null) {
            disconnectService.cancelDisconnectTimer(foundUser.getId());
        }
	}

	public void heartbeat(String token) {
    	User user = userRepository.findByToken(token);
    	if (user == null) return;
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant last = user.getLastHeartbeat();
        UserStatus resolvedStatus = resolveStatusForLogin(user.getId());
        boolean statusNeedsUpdate = user.getStatus() != resolvedStatus;
        boolean shouldSaveHeartbeat = last == null || now.isAfter(last.plusSeconds(HEARTBEAT_WRITE_THROTTLE_SECONDS));
        boolean shouldPersist = shouldSaveHeartbeat || statusNeedsUpdate;
        if (shouldPersist) {
            if (shouldSaveHeartbeat) {
                user.setLastHeartbeat(now);
            }
            if (statusNeedsUpdate) {
                user.setStatus(resolvedStatus);
            }
            userRepository.save(user);
        }
        if (disconnectService != null) {
            // A fresh authenticated heartbeat means the user is back and active.
            // Clear any stale "timed out in playing" flag to prevent false auto-Cabo.
            disconnectService.handleReconnect(user.getId());
        }
        if (statusNeedsUpdate) {
            onlineUsersEventPublisher.broadcastOnlineUsers();
        }
	}
}
