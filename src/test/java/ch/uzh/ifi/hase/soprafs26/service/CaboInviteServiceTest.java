package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CaboInviteRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CaboInviteCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CaboInviteServiceTest {

	@Mock
	private CaboInviteRepository caboInviteRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CaboInviteEventPublisher caboInviteEventPublisher;

	@Mock
	private LobbyService lobbyService;

	@InjectMocks
	private CaboInviteService caboInviteService;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void createInviteAsUser_noWaitingLobby_throwsConflict() {
		User host = new User();
		host.setId(1L);
		User invitee = new User();
		invitee.setId(2L);
		Mockito.when(userRepository.findByToken("token1")).thenReturn(host);
		Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
		Mockito.when(lobbyService.requireWaitingLobbyForHost(1L))
				.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Create a lobby first"));

		CaboInviteCreateDTO body = new CaboInviteCreateDTO();
		body.setToUserId(2L);

		assertThrows(ResponseStatusException.class,
				() -> caboInviteService.createInviteAsUser("token1", 1L, body));
	}

	@Test
	public void createInviteAsUser_inviteSelf_throwsBadRequest() {
		User host = new User();
		host.setId(1L);
		Mockito.when(userRepository.findByToken("token1")).thenReturn(host);

		CaboInviteCreateDTO body = new CaboInviteCreateDTO();
		body.setToUserId(1L);

		assertThrows(ResponseStatusException.class,
				() -> caboInviteService.createInviteAsUser("token1", 1L, body));
	}
}
