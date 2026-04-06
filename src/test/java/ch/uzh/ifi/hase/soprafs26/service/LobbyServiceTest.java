package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbySettingsPatchDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WaitingLobbyViewDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LobbyServiceTest {

	@Mock
	private LobbyRepository lobbyRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private LobbyEventPublisher lobbyEventPublisher;

	@InjectMocks
	private LobbyService lobbyService;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void updateLobbySettings_userIsHostAndLobbyIsWaiting_updatesIsPublic() {
		User host = new User();
		host.setId(1L);
		Mockito.when(userRepository.findByToken("token1")).thenReturn(host);

		Lobby lobby = new Lobby();
		lobby.setId(10L);
		lobby.setSessionId("S1");
		lobby.setSessionHostUserId(1L);
		lobby.setStatus("WAITING");
		lobby.setIsPublic(true);
		lobby.setPlayerIds(new ArrayList<>(List.of(1L)));
		Mockito.when(lobbyRepository.findBySessionId("S1")).thenReturn(lobby);
		Mockito.when(lobbyRepository.save(Mockito.any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));

		LobbySettingsPatchDTO body = new LobbySettingsPatchDTO();
		body.setIsPublic(false);

		Lobby updated = lobbyService.updateLobbySettings("token1", "S1", body);

		assertFalse(updated.getIsPublic());
		Mockito.verify(lobbyEventPublisher, Mockito.times(1)).broadcastLobbyUpdate(Mockito.eq(10L), Mockito.any());
	}

	@Test
	public void updateLobbySettings_notHost_throwsForbidden() {
		User other = new User();
		other.setId(2L);
		Mockito.when(userRepository.findByToken("token1")).thenReturn(other);

		Lobby lobby = new Lobby();
		lobby.setSessionId("S1");
		lobby.setSessionHostUserId(1L);
		lobby.setStatus("WAITING");
		lobby.setIsPublic(true);
		Mockito.when(lobbyRepository.findBySessionId("S1")).thenReturn(lobby);

		LobbySettingsPatchDTO body = new LobbySettingsPatchDTO();
		body.setIsPublic(false);

		assertThrows(ResponseStatusException.class, () -> lobbyService.updateLobbySettings("token1", "S1", body));
	}

	@Test
	public void getWaitingLobbyView_member_isPublicMatchesLobby() {
		User member = new User();
		member.setId(2L);
		member.setUsername("guest");
		Mockito.when(userRepository.findByToken("token1")).thenReturn(member);

		User host = new User();
		host.setId(1L);
		host.setUsername("host");
		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(host));
		Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(member));

		Lobby lobby = new Lobby();
		lobby.setId(10L);
		lobby.setSessionId("SID1");
		lobby.setSessionHostUserId(1L);
		lobby.setStatus("WAITING");
		lobby.setIsPublic(false);
		lobby.setPlayerIds(new ArrayList<>(List.of(1L, 2L)));
		Mockito.when(lobbyRepository.findBySessionId("SID1")).thenReturn(lobby);

		WaitingLobbyViewDTO dto = lobbyService.getWaitingLobbyView("token1", "SID1");

		assertFalse(dto.getIsPublic());
		assertEquals("SID1", dto.getSessionId());
		assertEquals(2, dto.getPlayers().size());
	}

	@Test
	public void joinLobby_invalidToken_throwsUnauthorized() {
		Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);

		// verify that an exception is thrown upon a lobby join with an invalid token
		// and save the exception for further verification of its status code
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> lobbyService.joinLobby("S1", "invalid-token"));

		// verify exception's status code
		assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
		// lobbyRepository should not be called after an attempt to join lobby with an invalid token
		Mockito.verify(lobbyRepository, Mockito.never()).findBySessionId(Mockito.anyString());
	}

	@Test
	public void joinLobby_validToken_addsUserToLobby() {
		User joiner = new User();
		joiner.setId(2L);
		Mockito.when(userRepository.findByToken("token")).thenReturn(joiner);

		Lobby lobby = new Lobby();
		lobby.setId(10L);
		lobby.setSessionId("S1");
		lobby.setSessionHostUserId(1L);
		lobby.setStatus("WAITING");
		lobby.setPlayerIds(new ArrayList<>(List.of(1L)));
		Mockito.when(lobbyRepository.findBySessionId("S1")).thenReturn(lobby);
		// upon calling lobbyRepository.save and passing to it an instance of Lobby
		// return the lobby that's being passed
		Mockito.when(lobbyRepository.save(Mockito.any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));

		Lobby result = lobbyService.joinLobby("S1", "token");

		assertTrue(result.getPlayerIds().contains(2L));
		// verify that broadcastLobbyUpdate was called once within the lobbyEventPublisher for lobby with id 10L
		Mockito.verify(lobbyEventPublisher, Mockito.times(1)).broadcastLobbyUpdate(Mockito.eq(10L), Mockito.any());
	}

	@Test
	public void joinLobby_invalidSessionId() {
		// create a user object
		User joiner = new User();
		joiner.setId(2L);
		Mockito.when(userRepository.findByToken("token")).thenReturn(joiner);

		// passing falseSessionId will will result in a null lobby since this sessionId doesnt exist
		// then we execute the method and assert it throws an exception
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
										() -> lobbyService.joinLobby("falseSessionId", "token"));

		// we check that the thrown exception is what we expect
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode);

		// verify that we never saved anything to the DB or broadcast an event
		Mockito.verify(lobbyRepository, Mockito.never()).save(Mockito.any());
		Mockito.verify(lobbyEventPublisher, Mockito.never()).broadcastLobbyUpdate(Mockito.anyLong(), Mockito.any());
	}

	@Test
	public void joinLobby_lobbyFull() {
		// create user to join
		User joiner = new User();
		joiner.setId(2L);
		Mockito.when(userRepository.findByToken("token")).thenReturn(joiner);

		// create lobby
		Lobby lobby = new Lobby();
		lobby.setId(10L);
		lobby.setSessionId("S1");
		lobby.setSessionHostUserId(1L);
		lobby.setStatus("WAITING");
		lobby.setPlayerIds(new ArrayList<>(List.of(1L, 3L, 4L, 5L)));
		// mock finding the lobby by session id to return the desired lobby
		Mockito.when(lobbyRepository.findBySessionId("S1")).thenReturn(lobby);
		// call joinLobby method and assert it throws an exception
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
										() -> lobbyService.joinLobby("S1", "token"));
		// make sure the exception is what we expect
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode);
		// verify external methods were called 
		Mockito.verify(lobbyRepository, Mockito.never()).save(Mockito.any());
		Mockito.verify(lobbyEventPublisher, Mockito.never()).broadcastLobbyUpdate(Mockito.anyLong(), Mockito.any());
	}

	@Test
	public void createLobby_validUser() {
		// setup creator
		User creator = new User();
		creator.setId(2L);
		Mockito.when(userRepository.findByToken("token")).thenReturn(creator);
		// mock active lobby check to return empty list meaning this user does not have an active lobby yet
		Mockito.when(lobbyRepository.findBySessionHostUserId(2L)).thenReturn(new ArrayList<>());
		// mock save method
		Mockito.when(lobbyRepository.save(Mockito.any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));

		Lobby result = lobbyService.createLobby("token", true);

		assertNotNull(result.getSessionId());
		assertTrue(2L, result.getSessionHostUserId());
		assertTrue(result.getIsPublic());
		assertTrue(result.getPlayerIds.contains(2L));
		// verify that external tools were called
		Mockito.verify(lobbyRepository, Mockito.times(1)).save(Mockito.any(Lobby.class));
		Mockito.verify(lobbyEventPublisher, Mockito.times(1)).broadcastLobbyUpdate(Mockito.any(), Mockito.any());
	}
}
