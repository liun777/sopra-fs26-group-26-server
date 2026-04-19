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
import org.springframework.http.MediaType;
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

    @Test
    void postDiscardPileDraw_callsService_returns204() throws Exception {
        String gameId = "g1";
        doNothing().when(gameService).moveDrawFromDiscardPile(anyString(), anyString());

        mockMvc.perform(post("/games/{gameId}/discard-pile/draw", gameId)
                        .header("Authorization", "token"))
                .andExpect(status().isNoContent());

        verify(gameService).moveDrawFromDiscardPile(eq(gameId), eq("token"));
    }

    @Test
    void postDiscardPileSwap_callsService_returns200() throws Exception {
        String gameId = "g1";
        doNothing().when(gameService).moveSwapWithDiscardPile(anyString(), anyString(), eq(2));

        mockMvc.perform(post("/games/{gameId}/discard-pile/swap", gameId)
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetCardIndex\":2}"))
                .andExpect(status().isOk());

        verify(gameService).moveSwapWithDiscardPile(eq(gameId), eq("token"), eq(2));
    }
}
