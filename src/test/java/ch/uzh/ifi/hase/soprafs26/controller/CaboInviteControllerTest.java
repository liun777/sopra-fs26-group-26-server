package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.CaboInvitePendingDTO;
import ch.uzh.ifi.hase.soprafs26.service.CaboInviteService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CaboInviteSentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CaboInviteCreateDTO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CaboInviteDecisionDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CaboInviteRespondDTO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

class CaboInviteControllerTest {

    private MockMvc mockMvc;
    private CaboInviteService caboInviteService;

    @BeforeEach
    void setup() {
        // Manually mock the service
        caboInviteService = Mockito.mock(CaboInviteService.class);
        
        // Inject it into the controller and build the standalone MockMvc
        CaboInviteController caboInviteController = new CaboInviteController(caboInviteService);
        mockMvc = MockMvcBuilders.standaloneSetup(caboInviteController).build();
    }

    @Test
    void getPending_invitesExist_returnsList() throws Exception {
        // 1. Setup: Create a list with two mock DTOs
        // (Since we don't know the exact fields inside CaboInvitePendingDTO, 
        // we can just use empty objects and check the array size!)
        List<CaboInvitePendingDTO> mockInvites = List.of(
                new CaboInvitePendingDTO(), 
                new CaboInvitePendingDTO()
        );

        Mockito.when(caboInviteService.getPendingInvitesForUser("valid-token", 1L))
               .thenReturn(mockInvites);

        // 2. Action & 3. Assertion
        mockMvc.perform(get("/users/1/invites")
                .header("Authorization", "valid-token"))
               .andExpect(status().isOk()) // 200 OK
               .andExpect(jsonPath("$.length()", is(2))); // Asserts the array has 2 items
    }

    @Test
    void getPending_noInvites_returnsEmptyList() throws Exception {
        // 1. Setup: The service returns an empty list
        Mockito.when(caboInviteService.getPendingInvitesForUser("valid-token", 1L))
               .thenReturn(List.of());

        // 2. Action & 3. Assertion
        mockMvc.perform(get("/users/1/invites")
                .header("Authorization", "valid-token"))
               .andExpect(status().isOk()) // 200 OK
               .andExpect(jsonPath("$.length()", is(0))); // Asserts the array is empty
    }

    @Test
    void getPending_invalidToken_throwsUnauthorized() throws Exception {
        // 1. Setup: The service rejects the request
        Mockito.when(caboInviteService.getPendingInvitesForUser("bad-token", 1L))
               .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // 2. Action & 3. Assertion
        mockMvc.perform(get("/users/1/invites")
                .header("Authorization", "bad-token"))
               .andExpect(status().isUnauthorized()); // 401 UNAUTHORIZED
    }

    @Test
    void getSentForHost_invitesExist_returnsList() throws Exception {
        // 1. Setup: Create a list of mock sent invites
        List<CaboInviteSentDTO> mockSentInvites = List.of(
                new CaboInviteSentDTO(), 
                new CaboInviteSentDTO()
        );

        Mockito.when(caboInviteService.getSentInvitesForUser("valid-token", 1L))
               .thenReturn(mockSentInvites);

        // 2. Action & 3. Assertion
        mockMvc.perform(get("/users/1/invites/sent")
                .header("Authorization", "valid-token"))
               .andExpect(status().isOk()) // 200 OK
               .andExpect(jsonPath("$.length()", is(2))); // Asserts the array has 2 items
    }

    @Test
    void getSentForHost_noInvites_returnsEmptyList() throws Exception {
        // 1. Setup: The service returns an empty list
        Mockito.when(caboInviteService.getSentInvitesForUser("valid-token", 1L))
               .thenReturn(List.of());

        // 2. Action & 3. Assertion
        mockMvc.perform(get("/users/1/invites/sent")
                .header("Authorization", "valid-token"))
               .andExpect(status().isOk()) // 200 OK
               .andExpect(jsonPath("$.length()", is(0))); // Asserts the array is empty
    }

    @Test
    void getSentForHost_invalidToken_throwsUnauthorized() throws Exception {
        // 1. Setup: The service rejects the request
        Mockito.when(caboInviteService.getSentInvitesForUser("bad-token", 1L))
               .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // 2. Action & 3. Assertion
        mockMvc.perform(get("/users/1/invites/sent")
                .header("Authorization", "bad-token"))
               .andExpect(status().isUnauthorized()); // 401 UNAUTHORIZED
    }

    @Test
    void createInvite_validRequest_returnsCreatedInvite() throws Exception {
        // 1. Setup: Create a mock response DTO
        CaboInvitePendingDTO mockResponse = new CaboInvitePendingDTO();
        // If you know a specific field in CaboInvitePendingDTO, you can set it here!
        // mockResponse.setInviteId(99L); 

        Mockito.when(caboInviteService.createInviteAsUser(
                Mockito.eq("valid-token"), 
                Mockito.eq(1L), 
                Mockito.any(CaboInviteCreateDTO.class)))
               .thenReturn(mockResponse);

        // A basic JSON payload to send in the request
        String jsonBody = """
                {
                    "targetUserId": 2
                }
                """;

        // 2. Action & 3. Assertion
        mockMvc.perform(post("/users/1/invites")
                .header("Authorization", "valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
               .andExpect(status().isCreated()); // 201 CREATED
               // .andExpect(jsonPath("$.inviteId", is(99))); // Uncomment if applicable
               
        // Verify the service was called exactly once with the correct token and userId
        Mockito.verify(caboInviteService, Mockito.times(1))
               .createInviteAsUser(Mockito.eq("valid-token"), Mockito.eq(1L), Mockito.any(CaboInviteCreateDTO.class));
    }

    @Test
    void createInvite_missingBody_returnsBadRequest() throws Exception {
        // 1. Setup: No mock setup needed because Spring will block this before it hits the service!

        // 2. Action & 3. Assertion
        mockMvc.perform(post("/users/1/invites")
                .header("Authorization", "valid-token")
                .contentType(MediaType.APPLICATION_JSON)) // Notice: No .content(...) !
               .andExpect(status().isBadRequest()); // 400 BAD REQUEST

        // Verify the service was entirely protected from the bad request
        Mockito.verify(caboInviteService, Mockito.never())
               .createInviteAsUser(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    void createInvite_invalidToken_throwsUnauthorized() throws Exception {
        // 1. Setup: The service rejects the token
        Mockito.when(caboInviteService.createInviteAsUser(
                Mockito.eq("bad-token"), 
                Mockito.eq(1L), 
                Mockito.any(CaboInviteCreateDTO.class)))
               .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        String jsonBody = "{}";

        // 2. Action & 3. Assertion
        mockMvc.perform(post("/users/1/invites")
                .header("Authorization", "bad-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
               .andExpect(status().isUnauthorized()); // 401 UNAUTHORIZED
    }

    @Test
    void respond_validRequest_returnsRespondDTO() throws Exception {
        // 1. Setup: Create a mock response DTO
        CaboInviteRespondDTO mockResponse = new CaboInviteRespondDTO();
        
        Mockito.when(caboInviteService.respondForUser(
                Mockito.eq("valid-token"), 
                Mockito.eq(1L), 
                Mockito.eq(99L), 
                Mockito.any(CaboInviteDecisionDTO.class)))
               .thenReturn(mockResponse);

        // A basic JSON payload (e.g., accepting the invite)
        String jsonBody = """
                {
                    "accepted": true
                }
                """;

        // 2. Action & 3. Assertion
        mockMvc.perform(patch("/users/1/invites/99")
                .header("Authorization", "valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
               .andExpect(status().isOk()); // 200 OK
               
        // Verify the service was called properly
        Mockito.verify(caboInviteService, Mockito.times(1))
               .respondForUser(Mockito.eq("valid-token"), Mockito.eq(1L), Mockito.eq(99L), Mockito.any(CaboInviteDecisionDTO.class));
    }

    @Test
    void respond_missingBody_returnsBadRequest() throws Exception {
        // 1. Setup: No service mock needed because Spring catches it first!

        // 2. Action & 3. Assertion
        mockMvc.perform(patch("/users/1/invites/99")
                .header("Authorization", "valid-token")
                .contentType(MediaType.APPLICATION_JSON)) // Notice: No .content()!
               .andExpect(status().isBadRequest()); // 400 BAD REQUEST

        Mockito.verify(caboInviteService, Mockito.never())
               .respondForUser(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    void respond_invalidToken_throwsUnauthorized() throws Exception {
        // 1. Setup: The service rejects the token
        Mockito.when(caboInviteService.respondForUser(
                Mockito.eq("bad-token"), 
                Mockito.eq(1L), 
                Mockito.eq(99L), 
                Mockito.any(CaboInviteDecisionDTO.class)))
               .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        String jsonBody = "{}";

        // 2. Action & 3. Assertion
        mockMvc.perform(patch("/users/1/invites/99")
                .header("Authorization", "bad-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
               .andExpect(status().isUnauthorized()); // 401 UNAUTHORIZED
    }

    @Test
    void deleteInvite_validRequest_returnsNoContent() throws Exception {
        // 1. Setup: Since the service method returns void, Mockito does nothing by default! 
        // We just let it run smoothly.

        // 2. Action & 3. Assertion
        mockMvc.perform(delete("/users/1/invites/99")
                .header("Authorization", "valid-token"))
               .andExpect(status().isNoContent()); // 204 NO CONTENT
               
        // Verify the service was actually told to delete the invite
        Mockito.verify(caboInviteService, Mockito.times(1))
               .deleteInviteForUser("valid-token", 1L, 99L);
    }

    @Test
    void deleteInvite_inviteNotFound_throwsNotFound() throws Exception {
        // 1. Setup: Tell Mockito to throw an error when a void method is called
        Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"))
               .when(caboInviteService).deleteInviteForUser("valid-token", 1L, 99L);

        // 2. Action & 3. Assertion
        mockMvc.perform(delete("/users/1/invites/99")
                .header("Authorization", "valid-token"))
               .andExpect(status().isNotFound()); // 404 NOT FOUND
    }

    @Test
    void deleteInvite_invalidToken_throwsUnauthorized() throws Exception {
        // 1. Setup: Tell Mockito to reject the bad token
        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
               .when(caboInviteService).deleteInviteForUser("bad-token", 1L, 99L);

        // 2. Action & 3. Assertion
        mockMvc.perform(delete("/users/1/invites/99")
                .header("Authorization", "bad-token"))
               .andExpect(status().isUnauthorized()); // 401 UNAUTHORIZED
    }
}