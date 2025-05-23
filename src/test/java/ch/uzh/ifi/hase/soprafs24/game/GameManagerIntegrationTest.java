package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@SpringBootTest(classes = GameManagerIntegrationTest.TestConfig.class)
public class GameManagerIntegrationTest {

    @Autowired
    private GameManager gameManager;

    @BeforeEach
    public void setup() {
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        LocalDate day1 = LocalDate.of(2025, 4, 9);
        LocalDate day2 = LocalDate.of(2025, 4, 10);
        LocalDate day3 = LocalDate.of(2025, 4, 11);
        timeline.put(day1, Map.of("AAPL", 100.0, "TSLA", 200.0));
        timeline.put(day2, Map.of("AAPL", 100.0, "TSLA", 200.0));
        timeline.put(day3, Map.of("AAPL", 100.0, "TSLA", 200.0));
        this.gameManager = new GameManager(1L, timeline, 999_999);
    }

    @Test
    public void nextRound_validTimeline_ranksPlayersCorrectly() {
        gameManager.registerPlayer(1L);
        gameManager.registerPlayer(2L);
        gameManager.registerPlayer(3L);
        gameManager.getPlayerStates().get(1L).setStock("AAPL", 10);
        gameManager.getPlayerStates().get(2L).setStock("TSLA", 20);
        gameManager.getPlayerStates().get(3L).setStock("AAPL", 50);
        gameManager.nextRound();
        List<LeaderBoardEntry> board = gameManager.getLeaderBoard();
        assertEquals(3, board.size());
        assertEquals(3L, board.get(0).getUserId());
        assertEquals(2L, board.get(1).getUserId());
        assertEquals(1L, board.get(2).getUserId());
    }

    @Test
    public void registerPlayer_duplicateId_throwsException() {
        gameManager.registerPlayer(1L);
        assertThrows(IllegalStateException.class,
                () -> gameManager.registerPlayer(1L));
    }

    @Configuration
    static class TestConfig {

        @Bean
        public GameManager gameManager() {
            LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
            LocalDate day1 = LocalDate.of(2025, 4, 9);
            timeline.put(day1, Map.of("AAPL", 100.0, "TSLA", 200.0));
            return new GameManager(1L, timeline, 999_999);
        }
    }
}
