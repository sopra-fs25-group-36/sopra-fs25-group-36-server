package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure JUnit integration test: GameManager + real PlayerState instances.
 * No Spring, no Mockito.  Runs entirely in memory.
 */
@DisplayName("GameManager ranks players by total assets")
class GameManagerUnitTest {

    private GameManager manager;

    @BeforeEach
    void setUp() {

        /* ----------  price timeline with ONE trading day ---------- */
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.of(2025, 4, 9),
                Map.of("AAPL", 100.0, "TSLA", 200.0));

        manager = new GameManager(1L, timeline, 1);   // gameId, timeline, maxRounds

        /* ----------  three players with different portfolios ---------- */
        manager.registerPlayer(1L);        // each starts with 10 000 cash
        manager.registerPlayer(2L);
        manager.registerPlayer(3L);

        manager.getPlayerStates().get(1L).setStock("AAPL", 10);   // +1 000
        manager.getPlayerStates().get(2L).setStock("TSLA", 20);   // +4 000
        manager.getPlayerStates().get(3L).setStock("AAPL", 50);   // +5 000
    }

    @Test
    void ranksPlayersByTotalAssets_highestFirst() {

        /* WHEN — close the round (GameManager recalculates the board) */
        manager.nextRound();

        List<LeaderBoardEntry> board = manager.getLeaderBoard();

        /* THEN — order must be 3 → 2 → 1 (15 000, 14 000, 11 000) */
        assertEquals(3,  board.size(),        "exactly three players expected");
        assertEquals(3L, board.get(0).getUserId(), "rank 1 should be player 3");
        assertEquals(2L, board.get(1).getUserId(), "rank 2 should be player 2");
        assertEquals(1L, board.get(2).getUserId(), "rank 3 should be player 1");
    }
}
