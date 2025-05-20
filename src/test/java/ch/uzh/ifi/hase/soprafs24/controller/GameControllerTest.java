package ch.uzh.ifi.hase.soprafs24.controller;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.service.GameService;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameService gameService;

    @Test
    public void startGame_validInput_returnsCreatedGame() throws Exception {
        Game game = new Game();
        game.setId(188L);
        game.setLobbyId(1L);

        given(gameService.tryStartGame(188L)).willReturn(game);

        MockHttpServletRequestBuilder postRequest = post("/game/{gameId}/start", 188L)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(game.getId().intValue())))
                .andExpect(jsonPath("$.lobbyId", is(game.getLobbyId().intValue())));
    }

    @Test
    public void startGame_serviceThrowsException_returnsExpectedErrorStatus() throws Exception {
        Long gameId = 188L;
        given(gameService.tryStartGame(gameId))
                .willThrow(new IllegalStateException("Cannot start game for testing purposes"));

        MockHttpServletRequestBuilder postRequest = post("/game/{gameId}/start", gameId)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void getGame_validInput_returnsGameManager() throws Exception {
        Long gameId = 188L;
        LinkedHashMap<LocalDate, Map<String, Double>> timelineData = new LinkedHashMap<>();
        Map<String, Double> day1Data = new LinkedHashMap<>();
        day1Data.put("AAPL", 150.0);
        timelineData.put(LocalDate.now(), day1Data);
        long roundDelayMillis = 60000L;

        GameManager realGameManager = new GameManager(gameId, timelineData, roundDelayMillis);

        given(gameService.getGame(gameId)).willReturn(realGameManager);

        MockHttpServletRequestBuilder getRequest = get("/game/{gameId}", gameId);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(realGameManager.getGameId().intValue())))
                .andExpect(jsonPath("$.stockTimeline", notNullValue()))
                .andExpect(jsonPath("$.active", is(realGameManager.isActive())))
                .andExpect(jsonPath("$.currentRound", is(realGameManager.getCurrentRound())))
                // Use the custom matcher for robust long comparison
                .andExpect(jsonPath("$.nextRoundStartTimeMillis",
                        isApproximately(realGameManager.getNextRoundStartTimeMillis())))
                .andExpect(jsonPath("$.playerStates").isEmpty())
                .andExpect(jsonPath("$.leaderBoard").isEmpty())
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }

    @Test
    public void getGame_gameNotFound_serviceReturnsNull_returnsOkWithEmptyContent() throws Exception {
        Long gameId = 189L;
        given(gameService.getGame(gameId)).willReturn(null);

        MockHttpServletRequestBuilder getRequest = get("/game/{gameId}", gameId);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void isGameActive_whenGameIsActive_returnsTrue() throws Exception {
        Long gameId = 1L;
        given(gameService.isGameActive(gameId)).willReturn(true);

        mockMvc.perform(get("/game/{gameId}/active", gameId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void isGameActive_whenGameIsNotActive_returnsFalse() throws Exception {
        Long gameId = 1L;
        given(gameService.isGameActive(gameId)).willReturn(false);

        mockMvc.perform(get("/game/{gameId}/active", gameId))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // Custom matcher to handle Long vs Integer from JSONPath
    private static Matcher<Object> isApproximately(long expectedValue) {
        return new org.hamcrest.BaseMatcher<Object>() {
            @Override
            public boolean matches(Object actual) {
                if (actual instanceof Number) {
                    return ((Number) actual).longValue() == expectedValue;
                }
                return false;
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("a Number approximately equal to ").appendValue(expectedValue);
            }
        };
    }

    // Custom matcher to handle Long vs Integer from JSONPath for comparisons
    private static Matcher<Object> isGreaterThanLong(long value) {
        return new org.hamcrest.BaseMatcher<Object>() {
            @Override
            public boolean matches(Object actual) {
                if (actual instanceof Number) {
                    return ((Number) actual).longValue() > value;
                }
                return false;
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("a Number greater than ").appendValue(value);
            }
        };
    }

    private static Matcher<Object> isLessThanOrEqualToLong(long value) {
        return new org.hamcrest.BaseMatcher<Object>() {
            @Override
            public boolean matches(Object actual) {
                if (actual instanceof Number) {
                    return ((Number) actual).longValue() <= value;
                }
                return false;
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("a Number less than or equal to ").appendValue(value);
            }
        };
    }

    @Test
    public void getGameRoundStatus_whenGameManagerExists_returnsGameStatusDTO() throws Exception {
        Long gameId = 1L;
        GameManager mockGameManager = mock(GameManager.class);
        when(mockGameManager.getCurrentRound()).thenReturn(5);
        when(mockGameManager.isActive()).thenReturn(true);
        long futureTime = System.currentTimeMillis() + 10000L;
        when(mockGameManager.getNextRoundStartTimeMillis()).thenReturn(futureTime);

        given(gameService.getGame(gameId)).willReturn(mockGameManager);

        mockMvc.perform(get("/game/{gameId}/round", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRound", is(5)))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.remainingTime").value(instanceOf(Number.class)))
                .andExpect(jsonPath("$.remainingTime", isLessThanOrEqualToLong(10000L)))
                .andExpect(jsonPath("$.remainingTime", isGreaterThanLong(0L)));
    }

    @Test
    public void getGameRoundStatus_whenGameManagerNotExists_returnsNotFound() throws Exception {
        Long gameId = 1L;
        given(gameService.getGame(gameId)).willReturn(null);

        mockMvc.perform(get("/game/{gameId}/round", gameId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getPlayerState_returnsOk() throws Exception {
        Long gameId = 1L;
        Long userId = 1L;

        GameManager mockGameManager = mock(GameManager.class);

        PlayerState playerState = new PlayerState(userId);

        given(gameService.getGame(gameId)).willReturn(mockGameManager);

        when(mockGameManager.getPlayerState(userId)).thenReturn(playerState);

        mockMvc.perform(get("/game/{gameId}/players/{userId}/state", gameId, userId))
                .andExpect(status().isOk());
    }

    @Test
    public void getPlayerState_gameNotFound_returns404() throws Exception {
        Long gameId = 1L;
        Long userId = 1L;

        given(gameService.getGame(gameId)).willReturn(null);

        mockMvc.perform(get("/game/{gameId}/players/{userId}/state", gameId, userId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getPlayerState_playerNotFound_returns404() throws Exception {
        Long gameId = 1L;
        Long userId = 1L;

        GameManager mockGameManager = mock(GameManager.class);

        given(gameService.getGame(gameId)).willReturn(mockGameManager);

        when(mockGameManager.getPlayerState(userId)).thenReturn(null);

        mockMvc.perform(get("/game/{gameId}/players/{userId}/state", gameId, userId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getRoundStatus_returnsOk() throws Exception {
        Long gameId = 1L;

        GameManager mockGameManager = mock(GameManager.class);

        given(gameService.getGame(gameId)).willReturn(mockGameManager);
        when(mockGameManager.getCurrentRound()).thenReturn(5);
        when(mockGameManager.haveAllPlayersSubmittedForCurrentRound()).thenReturn(true);
        long futureTime = System.currentTimeMillis() + 10000L;
        when(mockGameManager.getNextRoundStartTimeMillis()).thenReturn(futureTime);

        mockMvc.perform(get("/game/{gameId}/status", gameId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.allSubmitted", is(true)))
                .andExpect(jsonPath("$.roundEnded", is(true)))
                .andExpect(jsonPath("$.nextRoundStartTime", is(futureTime)));
    }

    @Test
    public void getRoundStatus_gameNotFound_returns404() throws Exception {
        Long gameId = 1L;

        given(gameService.getGame(gameId)).willReturn(null);

        mockMvc.perform(get("/game/{gameId}/status", gameId))
                .andExpect(status().isNotFound());
    }

}