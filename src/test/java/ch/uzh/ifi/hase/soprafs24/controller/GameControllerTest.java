package ch.uzh.ifi.hase.soprafs24.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.service.GameService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @Test
    public void startGame_validInput() throws Exception {
        // given
        Game game = new Game();
        game.setId(188L);

        given(gameService.tryStartGame(Mockito.any())).willReturn(game);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/game/{gameId}/start", game.getId());

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(game.getId().intValue())));

    }

     @Test
    public void getGame_validInput() throws Exception {
        // given
        Long gameId = 188L;
        GameManager gameManager = new GameManager(gameId, new LinkedHashMap<>(), 50);
        given(gameService.getGame(gameId)).willReturn(gameManager);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder getRequest = get("/game/{gameId}", gameId);

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk());    }

     }


