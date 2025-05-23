package ch.uzh.ifi.hase.soprafs24.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.LeaderBoardEntry;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderBoardEntryGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;

public class LeaderboardControllerTest {
    private GameService gameService;
    private LeaderBoardController controller;

    @BeforeEach
    public void setup() {
        gameService = mock(GameService.class);
        controller = new LeaderBoardController(gameService);

    }

    @Test
    public void testGetLeaderBoard_returnsCorrectDTOs() {
        Long gameId = 1L;
        GameManager mockGameManager = mock(GameManager.class);
        List<LeaderBoardEntry> mockLeaderboard = Arrays.asList(
                new LeaderBoardEntry(101L, 12345.67),
                new LeaderBoardEntry(102L, 11000.00));
        when(gameService.getGame(gameId)).thenReturn(mockGameManager);
        when(mockGameManager.getLeaderBoard()).thenReturn(mockLeaderboard);
        List<LeaderBoardEntryGetDTO> result = controller.getLeaderBoard(gameId);
        assertEquals(2, result.size());
        assertEquals(101L, result.get(0).getUserId());
        assertEquals(12345.67, result.get(0).getTotalAssets(), 0.01);
        assertEquals(102L, result.get(1).getUserId());
        assertEquals(11000.00, result.get(1).getTotalAssets(), 0.01);
    }

    @Test
    public void testGetLeaderBoard_emptyListIfNoPlayers() {
        Long gameId = 2L;
        GameManager mockGameManager = mock(GameManager.class);
        when(gameService.getGame(gameId)).thenReturn(mockGameManager);
        when(mockGameManager.getLeaderBoard()).thenReturn(List.of());
        List<LeaderBoardEntryGetDTO> result = controller.getLeaderBoard(gameId);
        assertTrue(result.isEmpty(), "Leaderboard should be empty if no players");
    }

    @Test
    public void testLeaderboardRanksPlayersByTotalAssets() {
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        LocalDate day1 = LocalDate.of(2025, 4, 9);
        LocalDate day2 = LocalDate.of(2025, 4, 10);
        LocalDate day3 = LocalDate.of(2025, 4, 11);
        Map<String, Double> pricesDay1 = Map.of("AAPL", 100.0, "TSLA", 200.0);
        timeline.put(day1, pricesDay1);
        timeline.put(day2, pricesDay1);
        timeline.put(day3, pricesDay1);
        GameManager manager = new GameManager(1L, timeline, 999999);
        manager.registerPlayer(1L);
        manager.registerPlayer(2L);
        manager.registerPlayer(3L);
        manager.getPlayerStates().get(1L).setStock("AAPL", 10);
        manager.getPlayerStates().get(2L).setStock("TSLA", 20);
        manager.getPlayerStates().get(3L).setStock("AAPL", 50);
        manager.nextRound();
        List<LeaderBoardEntry> board = manager.getLeaderBoard();
        assertEquals(3, board.size());
        assertEquals(3L, board.get(0).getUserId());
        assertEquals(2L, board.get(1).getUserId());
        assertEquals(1L, board.get(2).getUserId());

    }
}
