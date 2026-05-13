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
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final int MAX_BIO_LENGTH = 180;
    private static final List<String> CHARACTER_COLOR_ORDER = List.of(
            "navy_blue",
            "light_blue",
            "dark_green",
            "light_green",
            "yellow",
            "orange",
            "red",
            "pink",
            "purple");
    private static final List<String> APPEARANCE_MODE_ORDER = List.of(
            "system",
            "light",
            "dark");
    private static final String PREFERRED_COLOR_UNASSIGNED = "__unassigned__";
    private static final Map<String, String> LEGACY_COLOR_ALIAS_MAP = Map.ofEntries(
            Map.entry("black", "navy_blue"),
            Map.entry("blue", "navy_blue"),
            Map.entry("green", "light_green"),
            Map.entry("default", "orange"),
            Map.entry("slate", "navy_blue"),
            Map.entry("graphite", "dark_green"),
            Map.entry("forest", "dark_green"),
            Map.entry("ocean", "light_blue"),
            Map.entry("teal", "light_blue"),
            Map.entry("coral", "red"),
            Map.entry("indigo", "navy_blue"),
            Map.entry("plum", "purple"),
            Map.entry("amber", "yellow"));

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;
    private final SessionRepository sessionRepository;
	private final OnlineUsersEventPublisher onlineUsersEventPublisher;
    private final DisconnectService disconnectService;
    private final LobbyService lobbyService;

	public UserService(@Qualifier("userRepository") UserRepository userRepository,
                       @Qualifier("lobbyRepository") LobbyRepository lobbyRepository,
                       @Lazy SessionRepository sessionRepository,
	                   OnlineUsersEventPublisher onlineUsersEventPublisher,
                       @Lazy DisconnectService disconnectService,
                       @Lazy LobbyService lobbyService) {
		this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
        this.sessionRepository = sessionRepository;
		this.onlineUsersEventPublisher = onlineUsersEventPublisher;
		this.disconnectService = disconnectService;
        this.lobbyService = lobbyService;
	}

    private String normalizeCharacterColorId(String rawColorId) {
        String normalized = rawColorId == null ? "" : rawColorId.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return "";
        }
        if (LEGACY_COLOR_ALIAS_MAP.containsKey(normalized)) {
            normalized = LEGACY_COLOR_ALIAS_MAP.get(normalized);
        }
        return CHARACTER_COLOR_ORDER.contains(normalized) ? normalized : "";
    }

    private List<String> sanitizePreferredColorPriority(List<String> priorityList) {
        if (priorityList == null || priorityList.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String entry : priorityList) {
            String normalized = entry == null ? "" : entry.trim().toLowerCase();
            if (normalized.isEmpty() || PREFERRED_COLOR_UNASSIGNED.equals(normalized)) {
                continue;
            }
            normalized = normalizeCharacterColorId(normalized);
            if (normalized.isEmpty() || seen.contains(normalized)) {
                continue;
            }
            seen.add(normalized);
            sanitized.add(normalized);
        }
        return sanitized;
    }

    private String normalizeAppearanceMode(String rawAppearanceMode) {
        String normalized = rawAppearanceMode == null ? "" : rawAppearanceMode.trim().toLowerCase();
        return APPEARANCE_MODE_ORDER.contains(normalized) ? normalized : "";
    }

    private boolean isUserInPlayingLobby(Long userId) {
        if (userId == null || lobbyRepository == null) {
            return false;
        }
        // check only players, not spectators 
        return lobbyRepository.findByStatusAndParticipantId("PLAYING", userId).stream()
                .anyMatch(lobby -> lobby.getPlayerIds() != null && lobby.getPlayerIds().contains(userId));
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
        // then by number of played sessions, descending
        // then by average score over sessions, ascending
        // then by ids, ascending
        ordered.sort(Comparator
                .comparing(User::getGamesWon, Comparator.nullsFirst(Comparator.reverseOrder()))
                .thenComparing(
                        (User user) -> playedSessionsByUserId.getOrDefault(user.getId(), 0),
                        Comparator.reverseOrder()
                )
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
        if (newUser.getBio() != null) {
            String normalizedBio = newUser.getBio().trim();
            if (normalizedBio.length() > MAX_BIO_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bio must be 180 characters or fewer.");
            }
            newUser.setBio(normalizedBio);
        } else {
            newUser.setBio("");
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
    public void updateUser(Long userId, User userInput) {
        User user = getUserById(userId);
        boolean shouldRefreshLobbyPresentation = false;
        if (userInput.getPassword() != null) {
            user.setPassword(userInput.getPassword());
        }
        if (userInput.getBio() != null) {
            String normalizedBio = userInput.getBio().trim();
            if (normalizedBio.length() > MAX_BIO_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bio must be 180 characters or fewer.");
            }
            user.setBio(normalizedBio);
        }
        if (userInput.getStatus() != null) {
            user.setStatus(userInput.getStatus());
        }
        if (userInput.getIsPublicLog() != null) {
            user.setIsPublicLog(userInput.getIsPublicLog());
        }
        if (userInput.getProfileCharacterId() != null) {
            String nextCharacterId = userInput.getProfileCharacterId().trim();
            if (!nextCharacterId.isEmpty()) {
                user.setProfileCharacterId(nextCharacterId);
            }
        }
        if (userInput.getPreferredColorPriority() != null) {
            List<String> sanitizedPriority = sanitizePreferredColorPriority(userInput.getPreferredColorPriority());
            user.setPreferredColorPriority(sanitizedPriority);
            shouldRefreshLobbyPresentation = true;
        }
        if (userInput.getMenuBackgroundId() != null) {
            String nextMenuBackgroundId = userInput.getMenuBackgroundId().trim();
            if (!nextMenuBackgroundId.isEmpty()) {
                user.setMenuBackgroundId(nextMenuBackgroundId);
            }
        }
        if (userInput.getGameBackgroundId() != null) {
            String nextGameBackgroundId = userInput.getGameBackgroundId().trim();
            if (!nextGameBackgroundId.isEmpty()) {
                user.setGameBackgroundId(nextGameBackgroundId);
            }
        }
        if (userInput.getPrimaryColorId() != null) {
            String nextPrimaryColorId = normalizeCharacterColorId(userInput.getPrimaryColorId());
            if (!nextPrimaryColorId.isEmpty()) {
                user.setPrimaryColorId(nextPrimaryColorId);
                shouldRefreshLobbyPresentation = true;
            }
        }
        if (userInput.getAppearanceMode() != null) {
            String nextAppearanceMode = normalizeAppearanceMode(userInput.getAppearanceMode());
            if (!nextAppearanceMode.isEmpty()) {
                user.setAppearanceMode(nextAppearanceMode);
            }
        }
        if (userInput.getTutorialsEnabled() != null) {
            user.setTutorialsEnabled(userInput.getTutorialsEnabled());
        }
        if (userInput.getMusicVolume() != null) {
            int clampedMusicVolume = Math.max(0, Math.min(100, userInput.getMusicVolume()));
            user.setMusicVolume(clampedMusicVolume);
        }
        if (userInput.getSoundEffectsVolume() != null) {
            int clampedEffectsVolume = Math.max(0, Math.min(100, userInput.getSoundEffectsVolume()));
            user.setSoundEffectsVolume(clampedEffectsVolume);
        }
        if (userInput.getMusicBlacklist() != null) {
            List<String> sanitizedBlacklist = new ArrayList<>();
            for (String tag : userInput.getMusicBlacklist()) {
                String normalized = tag == null ? "" : tag.trim();
                if (!normalized.isEmpty()) {
                    sanitizedBlacklist.add(normalized);
                }
            }
            user.setMusicBlacklist(sanitizedBlacklist);
        }
        userRepository.save(user);
        userRepository.flush();
        if (shouldRefreshLobbyPresentation && lobbyService != null) {
            lobbyService.refreshWaitingLobbyPresentationForUser(user.getId());
        }
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

        // get playing lobby for this user
        List<Lobby> playing = lobbyRepository.findByStatusAndParticipantId("PLAYING", userId);
        if (!playing.isEmpty()) {
            // there can be only 1 playing lobby for a user
            Lobby l = playing.get(0);
            // if in the list of players -> playing status
            if (l.getPlayerIds() != null && l.getPlayerIds().contains(userId)) {
                return UserStatus.PLAYING;
            }
            // else  -> spectating status
            return UserStatus.SPECTATING;
        }

        // same logic as above but for waiting lobby
        List<Lobby> waiting = lobbyRepository.findByStatusAndParticipantId("WAITING", userId);
        if (!waiting.isEmpty()) {
            Lobby l = waiting.get(0);
            if (l.getPlayerIds() != null && l.getPlayerIds().contains(userId)) {
                return UserStatus.LOBBY;
            }
            return UserStatus.SPECTATING;
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
