package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    // sum users' scores across rounds so sessions' totalScores match round history
    private static Map<Long, Integer> totalScoresFromRounds(List<Map<Long, Integer>> perRound) {
        Map<Long, Integer> totals = new HashMap<>();
        // if no input given - return empty map
        if (perRound == null) {
            return totals;
        }
        // get every round's map ("user id - round score" pairs) from list of rounds
        for (Map<Long, Integer> roundMap : perRound) {
            // skip invalid maps
            if (roundMap == null) {
                continue;
            }
            // iterate every "user id - round score" pair from current round's map
            for (Map.Entry<Long, Integer> e : roundMap.entrySet()) {
                Long userId = e.getKey();
                Integer score = e.getValue();
                // skip invalid "user id - round score" pairs
                if (userId == null || score == null) {
                    continue;
                }
                // add current round's score to user's total score
                totals.merge(userId, score, Integer::sum);
            }
        }
        return totals;
    }

    private static void setSessionRoundScoresWithMatchingTotalScores(Session session, List<Map<Long, Integer>> rounds) {
        session.setUserScoresPerRound(rounds);
        session.setTotalScoreByUserId(new HashMap<>(totalScoresFromRounds(rounds)));
    }

	@Mock
	private UserRepository userRepository;

	@Mock
	private LobbyRepository lobbyRepository;

	@Mock
	private SessionRepository sessionRepository;

	@Mock
	private OnlineUsersEventPublisher onlineUsersEventPublisher;

	@Mock
	private DisconnectService disconnectService;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setId(1L);
		testUser.setName("testName");
		testUser.setUsername("testUsername");
		testUser.setPassword("testPassword");
		testUser.setCreationDate(LocalDate.now());

		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
	}

	@Test
	public void createUser_validInputs_success() {
		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		User createdUser = userService.createUser(testUser);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

		assertEquals(testUser.getId(), createdUser.getId());
		assertEquals(testUser.getName(), createdUser.getName());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertNotNull(createdUser.getToken());
		//assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
	}

// this test is commented out because UserService.java says we allow duplicate names if the username differs
/*
	@Test
	public void createUser_duplicateName_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}
*/

	@Test
	public void createUser_duplicateInputs_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void logoutUser_changesStatusToOffline_success() {
		User user = new User();
		user.setId(1L);
		user.setToken("token");
		user.setStatus(UserStatus.ONLINE);

		when(userRepository.findByToken("token")).thenReturn(user);
        when(lobbyRepository.existsByStatusAndPlayerId("PLAYING", 1L)).thenReturn(false);

		userService.logoutUser("token");
		
		assertEquals(UserStatus.OFFLINE, user.getStatus());
		verify(userRepository, Mockito.times(1)).save(user);
	}

    @Test
    public void logoutUser_whileInPlayingLobby_throwsConflict() {
        User user = new User();
        user.setId(7L);
        user.setToken("token");
        user.setStatus(UserStatus.PLAYING);

        Lobby playingLobby = new Lobby();
        playingLobby.setStatus("PLAYING");
        playingLobby.setPlayerIds(List.of(7L, 8L));

        when(userRepository.findByToken("token")).thenReturn(user);
        when(lobbyRepository.existsByStatusAndPlayerId("PLAYING", 7L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.logoutUser("token"));
        assertEquals(409, ex.getStatusCode().value());
        verify(userRepository, Mockito.never()).save(user);
    }

	@Test
    public void createUser_usernameTooLong_throwsBadRequest() {
        // 1. GIVEN: A user object with a 17-character username
        User newRestrictedUser = new User();
        newRestrictedUser.setUsername("12345678901234567"); // 17 chars!
        newRestrictedUser.setPassword("securePassword");

        // 2. WHEN / THEN: Attempting to save it throws an error
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> userService.createUser(newRestrictedUser)); // (Adjust method name if needed)

        // Verify the server rejects it with a 400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        
        // Verify the database was never touched
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void heartbeat_offlineUserWithoutLobby_setsOnline() {
        User user = new User();
        user.setId(10L);
        user.setToken("token-online");
        user.setStatus(UserStatus.OFFLINE);
        user.setLastHeartbeat(Instant.now());

        when(userRepository.findByToken("token-online")).thenReturn(user);
        when(lobbyRepository.existsByStatusAndPlayerId("PLAYING", 10L)).thenReturn(false);
        when(lobbyRepository.existsByStatusAndPlayerId("WAITING", 10L)).thenReturn(false);

        userService.heartbeat("token-online");

        assertEquals(UserStatus.ONLINE, user.getStatus());
        verify(userRepository, Mockito.atLeastOnce()).save(user);
        verify(onlineUsersEventPublisher, Mockito.times(1)).broadcastOnlineUsers();
        verify(disconnectService, Mockito.times(1)).handleReconnect(10L);
    }

    @Test
    public void heartbeat_offlineUserInWaitingLobby_setsLobby() {
        User user = new User();
        user.setId(11L);
        user.setToken("token-lobby");
        user.setStatus(UserStatus.OFFLINE);
        user.setLastHeartbeat(Instant.now());

        when(userRepository.findByToken("token-lobby")).thenReturn(user);
        when(lobbyRepository.existsByStatusAndPlayerId("PLAYING", 11L)).thenReturn(false);
        when(lobbyRepository.existsByStatusAndPlayerId("WAITING", 11L)).thenReturn(true);

        userService.heartbeat("token-lobby");

        assertEquals(UserStatus.LOBBY, user.getStatus());
        verify(userRepository, Mockito.atLeastOnce()).save(user);
        verify(onlineUsersEventPublisher, Mockito.times(1)).broadcastOnlineUsers();
        verify(disconnectService, Mockito.times(1)).handleReconnect(11L);
    }

    @Test
    public void heartbeat_offlineUserInPlayingLobby_setsPlaying() {
        User user = new User();
        user.setId(12L);
        user.setToken("token-playing");
        user.setStatus(UserStatus.OFFLINE);
        user.setLastHeartbeat(Instant.now());

        when(userRepository.findByToken("token-playing")).thenReturn(user);
        when(lobbyRepository.existsByStatusAndPlayerId("PLAYING", 12L)).thenReturn(true);

        userService.heartbeat("token-playing");

        assertEquals(UserStatus.PLAYING, user.getStatus());
        verify(userRepository, Mockito.atLeastOnce()).save(user);
        verify(onlineUsersEventPublisher, Mockito.times(1)).broadcastOnlineUsers();
        verify(disconnectService, Mockito.times(1)).handleReconnect(12L);
    }

    // in this test we set scores and respective totals directly 
    // because setting scores and inferring totals from them in gameplay flow is in the scope of another task
    @Test
    public void getUsers_recalculateRankingFromEndedSessions_updatesWinsAverageAndRank() {
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("u1");
        u1.setPassword("pw");
        u1.setCreationDate(LocalDate.now());

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("u2");
        u2.setPassword("pw");
        u2.setCreationDate(LocalDate.now());

        User u3 = new User();
        u3.setId(3L);
        u3.setUsername("u3");
        u3.setPassword("pw");
        u3.setCreationDate(LocalDate.now());

        // when we query repository for all users, return the 3 users
        when(userRepository.findAll()).thenReturn(new ArrayList<>(List.of(u1, u2, u3)));
        // when we save a user via repository - return the user that is passed as argument
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session ended1 = new Session();
        ended1.setEnded(true);
        List<Map<Long, Integer>> ended1Rounds = new ArrayList<>();
        // first round. user1: 10; user2: 60. user1 wins
        ended1Rounds.add(new HashMap<>(Map.of(1L, 10, 2L, 60)));
        // second round. user1: 10; user2: -10 (100 -> 50 rule). user2 wins
        ended1Rounds.add(new HashMap<>(Map.of(1L, 10, 2L, -10)));
        // total scores. user1: 20; user2: 50. user1 wins session/game
        setSessionRoundScoresWithMatchingTotalScores(ended1, ended1Rounds);

        // second session
        Session ended2 = new Session();
        ended2.setEnded(true);
        List<Map<Long, Integer>> ended2Rounds = new ArrayList<>();
        // one round only. user1: 40; user2: 5; user3: 25. user2 wins round and session
        ended2Rounds.add(new HashMap<>(Map.of(1L, 40, 2L, 5, 3L, 25)));
        setSessionRoundScoresWithMatchingTotalScores(ended2, ended2Rounds);

        // third session, not ended
        Session open = new Session();
        open.setEnded(false);
        // no rounds played yet - empty scores
        open.setTotalScoreByUserId(new HashMap<>());

        // when session repository is queried for all sessions - return 3 created sessions
        when(sessionRepository.findAll()).thenReturn(List.of(ended1, ended2, open));
        // getUsers() calls recalculateGlobalRankingFromSessions()
        List<User> users = userService.getUsers();

        assertEquals(3, users.size());
        assertEquals(1, u1.getGamesWon());
        assertEquals(1, u2.getGamesWon());
        assertEquals(0, u3.getGamesWon());
        assertEquals(30, u1.getAverageScorePerSession());
        assertEquals(28, u2.getAverageScorePerSession());
        assertEquals(25, u3.getAverageScorePerSession());
        assertEquals(1, u1.getRoundsWon());
        assertEquals(2, u2.getRoundsWon());
        assertEquals(0, u3.getRoundsWon());
        assertEquals(20, u1.getAverageScorePerRound());
        assertEquals(18, u2.getAverageScorePerRound());
        assertEquals(25, u3.getAverageScorePerRound());
        // user1 and user2 have same number of games won
        // but user2 has a lower average score per session
        assertEquals(2, u1.getOverallRank());
        assertEquals(1, u2.getOverallRank());
        assertEquals(3, u3.getOverallRank());
    }

    // user3 joins session during 2nd round (round index=1)
    // average score / win stats over rounds and sessions are consistent 
    // (if user is missing in a round, that round is ignored for that user)
    @Test
    public void getUsers_userJoinsDuringSecondRound_roundAndSessionMetricsAreCorrect() {
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("u1");
        u1.setPassword("pw");
        u1.setCreationDate(LocalDate.now());

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("u2");
        u2.setPassword("pw");
        u2.setCreationDate(LocalDate.now());

        User u3 = new User();
        u3.setId(3L);
        u3.setUsername("u3");
        u3.setPassword("pw");
        u3.setCreationDate(LocalDate.now());

        // when we query repository for all users, return the 3 users
        when(userRepository.findAll()).thenReturn(new ArrayList<>(List.of(u1, u2, u3)));
        // when we save a user via repository - return the user that is passed as argument
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session ended = new Session();
        ended.setEnded(true);
        List<Map<Long, Integer>> rounds = new ArrayList<>();
        // round1. user1: 10; user2: 20
        // user1 wins round
        rounds.add(new HashMap<>(Map.of(1L, 10, 2L, 20)));
        // round2. user1: 15; user2: 10; user3: 5
        // user3 wins round and session
        rounds.add(new HashMap<>(Map.of(1L, 15, 2L, 10, 3L, 5)));
        setSessionRoundScoresWithMatchingTotalScores(ended, rounds);

        // when querying session repository for sessions, return created session
        when(sessionRepository.findAll()).thenReturn(List.of(ended));
        // getUsers() calls recalculateGlobalRankingFromSessions()
        userService.getUsers();

        assertEquals(0, u1.getGamesWon());
        assertEquals(0, u2.getGamesWon());
        assertEquals(1, u3.getGamesWon());
        assertEquals(25, u1.getAverageScorePerSession());
        assertEquals(30, u2.getAverageScorePerSession());
        assertEquals(5, u3.getAverageScorePerSession());

        assertEquals(1, u1.getRoundsWon());
        assertEquals(0, u2.getRoundsWon());
        assertEquals(1, u3.getRoundsWon());
        assertEquals(13, u1.getAverageScorePerRound());
        assertEquals(15, u2.getAverageScorePerRound());
        assertEquals(5, u3.getAverageScorePerRound());

        // user3 won 1 game -> rank 1
        // user1 and user2 won 0 games
        // user1 has lower session average than user2
        assertEquals(2, u1.getOverallRank());
        assertEquals(3, u2.getOverallRank());
        assertEquals(1, u3.getOverallRank());
    }

    @Test
    public void getUsers_ongoingSessionRoundsAffectRoundBasedMetricsOnly_notSessionBasedMetrics() {
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("u1");
        u1.setPassword("pw");
        u1.setCreationDate(LocalDate.now());

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("u2");
        u2.setPassword("pw");
        u2.setCreationDate(LocalDate.now());

        // when we query repository for all users, return the 2 users
        when(userRepository.findAll()).thenReturn(new ArrayList<>(List.of(u1, u2)));
        // when we save a user via repository - return the user that is passed as argument
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // a session that has not ended yet
        Session ongoing = new Session();
        ongoing.setEnded(false);
        List<Map<Long, Integer>> rounds = new ArrayList<>();
        // round1. user1: 4; user2: 10
        // user1 wins round but not session (since its ongoing)
        rounds.add(new HashMap<>(Map.of(1L, 4, 2L, 10)));
        setSessionRoundScoresWithMatchingTotalScores(ongoing, rounds);

        // when querying session repository for sessions, return created session
        when(sessionRepository.findAll()).thenReturn(List.of(ongoing));
        // getUsers() calls recalculateGlobalRankingFromSessions()
        userService.getUsers();

        // neither user has won the game - session has not ended yet
        assertEquals(0, u1.getGamesWon());
        assertEquals(0, u2.getGamesWon());
        assertEquals(0, u1.getAverageScorePerSession());
        assertEquals(0, u2.getAverageScorePerSession());
        // round based metrics can be computed from an ongoing session
        assertEquals(1, u1.getRoundsWon());
        assertEquals(0, u2.getRoundsWon());
        assertEquals(4, u1.getAverageScorePerRound());
        assertEquals(10, u2.getAverageScorePerRound());
    }

    @Test
    public void getUsers_noRoundsAndEndedSessions_setsAllScoreStatsFieldsToZeroExceptRank() {
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("u1");
        u1.setPassword("pw");
        u1.setCreationDate(LocalDate.now());

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("u2");
        u2.setPassword("pw");
        u2.setCreationDate(LocalDate.now());

        // when we query repository for all users, return the 2 users
        when(userRepository.findAll()).thenReturn(new ArrayList<>(List.of(u1, u2)));
        // when we save a user via repository - return the user that is passed as argument
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // session that has not ended yet
        Session open = new Session();
        open.setEnded(false);
        // no rounds, empty total scores
        open.setTotalScoreByUserId(new HashMap<>());
        
        // when querying session repository for sessions, return created session
        when(sessionRepository.findAll()).thenReturn(List.of(open));
        // getUsers() calls recalculateGlobalRankingFromSessions()
        userService.getUsers();

        // all metrics equal to 0
        assertEquals(0, u1.getGamesWon());
        assertEquals(0, u2.getGamesWon());
        assertEquals(0, u1.getAverageScorePerSession());
        assertEquals(0, u2.getAverageScorePerSession());
        assertEquals(0, u1.getRoundsWon());
        assertEquals(0, u2.getRoundsWon());
        assertEquals(0, u1.getAverageScorePerRound());
        assertEquals(0, u2.getAverageScorePerRound());
        // ranking falls back to a ranking based on user ids
        assertEquals(1, u1.getOverallRank());
        assertEquals(2, u2.getOverallRank());
    }

}
