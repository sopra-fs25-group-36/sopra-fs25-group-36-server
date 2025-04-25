package ch.uzh.ifi.hase.soprafs24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

/**
 * REST-interface tests for {@link LobbyController}.
 */
@WebMvcTest(LobbyController.class)
class LobbyControllerRestTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @MockBean
    private LobbyService lobbyService;

    /* ------------------------------------------------------------------
     * POST /{userId}/createLobby
     * ------------------------------------------------------------------ */
    @Test
    void createLobby_setsCreatorAndTimeLimit() throws Exception {

        long creatorId = 1L;

        Lobby lobby = new Lobby();
        lobby.setId(10L);
        lobby.setTimeLimitSeconds(300L);
        lobby.setCreatedAt(Instant.now());
        lobby.setActive(true);
        lobby.setPlayerReadyStatuses(Map.of(creatorId, false));

        when(lobbyService.createLobby(eq(creatorId), any(Lobby.class)))
                .thenReturn(lobby);

        mockMvc.perform(post("/{userId}/createLobby", creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(10L));
    }

    /* ------------------------------------------------------------------
     * POST /lobby/{lobbyId}/joinLobby
     * ------------------------------------------------------------------ */
    @Test
    void joinLobby_addsUserWithReadyFalse() throws Exception {

        long lobbyId = 10L;
        long newUser = 2L;

        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);
        lobby.setTimeLimitSeconds(300L);
        lobby.setCreatedAt(Instant.now());
        lobby.setActive(true);
        lobby.setPlayerReadyStatuses(Map.of(newUser, false));

        when(lobbyService.addUserToLobby(lobbyId, newUser)).thenReturn(lobby);

        mockMvc.perform(post("/lobby/{lobbyId}/joinLobby", lobbyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(lobbyId))
               .andExpect(jsonPath("$.playerReadyStatuses['2']").value(false));
    }

    /* ------------------------------------------------------------------
     * GET /lobby/{lobbyId}
     * ------------------------------------------------------------------ */
    @Test
    void getLobby_returnsLobby() throws Exception {

        long lobbyId = 11L;

        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);
        lobby.setTimeLimitSeconds(300L);
        lobby.setCreatedAt(Instant.now());
        lobby.setActive(true);
        lobby.setPlayerReadyStatuses(Map.of());

        when(lobbyService.getLobbyById(lobbyId)).thenReturn(lobby);

        mockMvc.perform(get("/lobby/{lobbyId}", lobbyId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(lobbyId));
    }

    /* ------------------------------------------------------------------
     * POST /lobby/{lobbyId}/ready
     * ------------------------------------------------------------------ */
    @Test
    void setUserReady_marksFlagTrue() throws Exception {

        long lobbyId = 20L;
        long userId  = 5L;

        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);
        lobby.setTimeLimitSeconds(300L);
        lobby.setCreatedAt(Instant.now());
        lobby.setActive(true);
        lobby.setPlayerReadyStatuses(Map.of(userId, true));

        when(lobbyService.setUserReady(lobbyId, userId)).thenReturn(lobby);

        mockMvc.perform(post("/lobby/{lobbyId}/ready", lobbyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.playerReadyStatuses['5']").value(true));
    }
}
