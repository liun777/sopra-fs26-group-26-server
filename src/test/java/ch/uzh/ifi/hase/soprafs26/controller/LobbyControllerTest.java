package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LobbyService lobbyService;

	@MockitoBean
	private GameService gameService;

	@Test
	public void patchLobbySettings_validBody_returnsLobby() throws Exception {
		Lobby lobby = new Lobby();
		lobby.setId(1L);
		lobby.setSessionId("ABCD12EF");
		lobby.setSessionHostUserId(1L);
		lobby.setIsPublic(false);
		lobby.setStatus("WAITING");
		lobby.getPlayerIds().add(1L);

		given(lobbyService.updateLobbySettings(eq("my-token"), eq("ABCD12EF"), any()))
				.willReturn(lobby);

		MockHttpServletRequestBuilder patchRequest = patch("/lobbies/ABCD12EF/settings")
				.header("Authorization", "my-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(java.util.Map.of("isPublic", false)));

		mockMvc.perform(patchRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId", is(lobby.getSessionId())))
				.andExpect(jsonPath("$.isPublic", is(false)));
	}

	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e));
		}
	}
}
