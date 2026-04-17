package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.config.GameMoveAuthorizationInterceptor;
import ch.uzh.ifi.hase.soprafs26.config.GameMoveWebConfig;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
@Import({GameMoveWebConfig.class, GameMoveAuthorizationInterceptor.class})
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private LobbyService lobbyService;

    @Test
    void postMoveDraw_interceptorAllows_returns204() throws Exception {
        String gameId = "id1";

        doNothing().when(gameService).verifyMoveCallerIsCurrentPlayer(anyString(), anyString());

        mockMvc.perform(post("/games/{gameId}/moves/draw", gameId)
                        .header("Authorization", "token"))
                .andExpect(status().isNoContent());

        verify(gameService).verifyMoveCallerIsCurrentPlayer(eq(gameId), eq("token"));
        verify(gameService).moveDrawFromDrawPile(eq(gameId));
    }

    @Test
    void postMoveCabo_interceptorForbidden_doesNotCallMoveHandler() throws Exception {
        String gameId = "id1";

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn"))
                .when(gameService).verifyMoveCallerIsCurrentPlayer(anyString(), anyString());

        mockMvc.perform(post("/games/{gameId}/moves/cabo", gameId)
                        .header("Authorization", "token"))
                .andExpect(status().isForbidden());

        verify(gameService, never()).moveCallCabo(anyString(), anyString());
    }
}
