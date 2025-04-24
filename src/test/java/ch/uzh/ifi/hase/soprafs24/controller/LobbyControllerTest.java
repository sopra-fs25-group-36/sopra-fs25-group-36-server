package ch.uzh.ifi.hase.soprafs24.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * REST-interface test for {@link LobbyController} using the real controller,
 * MockMvc, and a mocked {@link LobbyService}.
 */
@WebMvcTest(LobbyController.class)
class LobbyControllerRestTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @MockBean
    private LobbyService lobbyService;

     //POST /{userId}/createLobby
    @Test
    void createLobby_setsCreatorAndTimeLimit() throws Exception {

        long creatorId = 1L;

        Lobby lobby = new Lobby();
        lobby.setId(10L);
        lobby.setTimeLimitSeconds(300L);
        lobby.setCreatedAt(LocalDateTime.now());
        lobby.setActive(true);

        Map<Long, Boolean> readyMap = new HashMap<>();
        readyMap.put(creatorId, false);
        lobby.setPlayerReadyStatuses(readyMap);

        when(lobbyService.createLobby(eq(creatorId), any(Lobby.class)))
                .thenReturn(lobby);

//        String body = """
////            {"lobbyName":"Sprint Game"}
//            """;

        mockMvc.perform(post("/{userId}/createLobby", creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))                 // ← supply “something” to deserialize
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }

     //POST /{lobbyId}/joinLobby
    @Test
    void joinLobby_addsUserWithReadyFalse() throws Exception {

        long lobbyId = 10L;
        long newUser = 2L;

        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);
        lobby.setTimeLimitSeconds(300L);
        lobby.setPlayerReadyStatuses(Map.of(newUser, false));

        when(lobbyService.addUserToLobby(lobbyId, newUser)).thenReturn(lobby);

        String body = """
            {"userId": 2}
            """;

        mockMvc.perform(post("/{lobbyId}/joinLobby", lobbyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lobbyId))
                .andExpect(jsonPath("$.playerReadyStatuses['2']").value(false));
    }

     /// GET /lobby/{lobbyId}
    @Test
    void getLobby_returnsLobby() throws Exception {

        long lobbyId = 11L;
        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);

        when(lobbyService.getLobbyById(lobbyId)).thenReturn(lobby);

        mockMvc.perform(get("/lobby/{lobbyId}", lobbyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lobbyId));
    }

    //POST /{lobbyId}/ready
    @Test
    void setUserReady_marksFlagTrue() throws Exception {

        long lobbyId = 20L;
        long userId  = 5L;

        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);
        lobby.setPlayerReadyStatuses(Map.of(userId, true));

        when(lobbyService.setUserReady(lobbyId, userId)).thenReturn(lobby);

        String body = """
            {"userId": 5}
            """;

        mockMvc.perform(post("/{lobbyId}/ready", lobbyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerReadyStatuses['5']").value(true));
    }
}
