package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;

@SpringBootTest
@Transactional
public class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private LobbyRepository lobbyRepository;

    @BeforeEach
    void setUp() {
        InMemoryGameRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        InMemoryGameRegistry.clear();
    }

    @Test
    public void testTryStartGame_success() {
        Lobby lobby = new Lobby();
        Map<Long, Boolean> players = new HashMap<>();
        players.put(1L, true);
        players.put(2L, true);
        lobby.setPlayerReadyStatuses(players);
        lobby.setTimeLimitSeconds(60L);
        lobby.setActive(true);
        lobby = lobbyRepository.saveAndFlush(lobby);
        Game game = gameService.tryStartGame(lobby.getId());
        assertNotNull(game, "The returned game object should not be null.");
        assertNotNull(game.getId(), "The game ID should not be null after creation.");
        assertTrue(InMemoryGameRegistry.isGameActive(game.getId()),
                "The game should be active in InMemoryGameRegistry after a successful start.");
        GameManager activeGameManager = InMemoryGameRegistry.getGame(game.getId());
        assertNotNull(activeGameManager, "A GameManager instance should be found in the registry.");
        assertEquals(game.getId(), activeGameManager.getGameId(),
                "The GameManager's ID should match the Game entity's ID.");
    }

    @Test
    public void testTryStartGame_playersNotReady_shouldFail() {
        Lobby lobby = new Lobby();
        Map<Long, Boolean> players = new HashMap<>();
        players.put(1L, true);
        players.put(2L, false);
        lobby.setPlayerReadyStatuses(players);
        lobby.setTimeLimitSeconds(60L);
        lobby.setActive(true);
        lobby = lobbyRepository.saveAndFlush(lobby);
        Lobby finalLobby = lobby;
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            gameService.tryStartGame(finalLobby.getId());
        }, "tryStartGame should throw IllegalStateException if not all players are ready.");

    }

    @Test
    public void testGameAutoFinishesAfter10Rounds() throws InterruptedException {
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        LocalDate baseDate = LocalDate.of(2025, 4, 1);
        int numberOfRounds = 10;
        for (int i = 0; i < numberOfRounds; i++) {
            Map<String, Double> snapshot = new HashMap<>();
            snapshot.put("AAPL", 150.0 + i);
            timeline.put(baseDate.plusDays(i), snapshot);
        }
        Long testGameId = 999L;
        int roundDurationSeconds = 1;
        GameManager gameManager = new GameManager(testGameId, timeline, roundDurationSeconds);
        gameManager.registerPlayer(1L);
        InMemoryGameRegistry.registerGame(testGameId, gameManager);
        assertTrue(InMemoryGameRegistry.isGameActive(testGameId),
                "Game should be active in the registry immediately after registration.");
        gameManager.startGame();
        long processingBufferMillis = 3000;
        long totalWaitTimeMillis = ((long) numberOfRounds * roundDurationSeconds * 1000) + processingBufferMillis;
        Thread.sleep(totalWaitTimeMillis);
        assertFalse(InMemoryGameRegistry.isGameActive(testGameId),
                "Game should be removed from InMemoryGameRegistry after " + numberOfRounds + " rounds are completed. " +
                        "Current round reported by GameManager: " + gameManager.getCurrentRound() +
                        ". Game active in registry: " + InMemoryGameRegistry.isGameActive(testGameId));
        assertEquals(numberOfRounds, gameManager.getCurrentRound(),
                "GameManager should have processed all " + numberOfRounds + " rounds.");

    }
}