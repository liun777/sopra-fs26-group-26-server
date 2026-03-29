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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		lobby.getPlayerIds().add(1L);
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
		lobby.getPlayerIds().add(1L);
		lobby.getPlayerIds().add(2L);
		Mockito.when(lobbyRepository.findBySessionId("SID1")).thenReturn(lobby);

		WaitingLobbyViewDTO dto = lobbyService.getWaitingLobbyView("token1", "SID1");

		assertFalse(dto.getIsPublic());
		assertEquals("SID1", dto.getSessionId());
		assertEquals(2, dto.getPlayers().size());
	}
}
