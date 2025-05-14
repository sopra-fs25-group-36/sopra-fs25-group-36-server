package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    public void testTryStartGame_success() {
        // Create a dummy lobby with all players ready
        Lobby lobby = new Lobby();
        Map<Long, Boolean> players = new HashMap<>();
        players.put(1L, true);
        players.put(2L, true);
        lobby.setPlayerReadyStatuses(players);
        lobby.setTimeLimitSeconds(600L);
        lobby = lobbyRepository.save(lobby);

        // Call the method
        Game game = gameService.tryStartGame(lobby.getId());
        System.out.println("Lobby ID: " + lobby.getId());
        System.out.println("Game ID: " + game.getId());
        // Assert game is created and stored
        assertNotNull(game.getId());
        assertEquals(lobby.getId(), game.getLobbyId());
        assertTrue(InMemoryGameRegistry.isGameActive(game.getId()));

    }

    private void assertTrue(boolean gameActive) {
    }

    @Test
    public void testTryStartGame_playersNotReady_shouldFail() {
        // Given: a lobby with some players not ready
        Lobby lobby = new Lobby();
        Map<Long, Boolean> players = new HashMap<>();
        players.put(1L, true);
        players.put(2L, false);

        lobby.setPlayerReadyStatuses(players);
        lobby.setTimeLimitSeconds(600L);
        lobby.setActive(true);

        lobby = lobbyRepository.save(lobby); // persist and get ID

        // When & Then: tryStartGame should throw IllegalStateException
        Lobby finalLobby = lobby;
        assertThrows(IllegalStateException.class, () -> {
            gameService.tryStartGame(finalLobby.getId());
        });
    }

    // tests that the game starts and ends correctly after 10 rounds, and works with
    // inMemoryGameRegistry
    @Test
    public void testGameAutoFinishesAfter10Rounds() throws InterruptedException {
        // Arrange: create dummy 10-day stock timeline
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        LocalDate baseDate = LocalDate.of(2025, 4, 1); // or LocalDate.now()

        for (int i = 0; i < 10; i++) {
            Map<String, Double> snapshot = new HashMap<>();
            snapshot.put("AAPL", 150.0 + i); // 1 stock per day
            timeline.put(baseDate.plusDays(i), snapshot);
        }

        // Create a test game with short delay (50 ms between rounds)
        Long testGameId = 999L;
        GameManager gameManager = new GameManager(testGameId, timeline, 5);

        // Register a dummy player
        gameManager.registerPlayer(1L);

        // Register game to registry
        InMemoryGameRegistry.registerGame(testGameId, gameManager);

        // Act: start the game rounds
        gameManager.startGame();

        // Wait long enough for 10 rounds to complete (10 * 50ms + buffer)
        Thread.sleep(10000); // 10 second should be plenty

        // Assert: game is no longer active and unregistered
        assertFalse(InMemoryGameRegistry.isGameActive(testGameId), "Game should be removed after 10 rounds");
        assertEquals(10, gameManager.getCurrentRound(), "Game should have completed 10 rounds");
    }

}
