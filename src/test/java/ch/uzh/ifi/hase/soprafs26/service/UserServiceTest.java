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
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private LobbyRepository lobbyRepository;

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
		testUser.setCreationDate(java.time.LocalDate.now());

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

}
