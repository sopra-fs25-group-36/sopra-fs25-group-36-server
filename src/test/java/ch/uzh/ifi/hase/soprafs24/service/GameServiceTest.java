package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository; // Not used directly, but GameService might use it
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional; // For DB rollback

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*; // Import static assertions from JUnit Jupiter

@SpringBootTest
@Transactional // Rolls back database transactions after each test
public class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private LobbyRepository lobbyRepository;

    // @Autowired
    // private GameRepository gameRepository; // Autowire if direct interaction is needed for assertions

    @BeforeEach
    void setUp() {
        // Clear the in-memory game registry before each test to ensure a clean state
        InMemoryGameRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear the in-memory game registry after each test as good practice,
        // although @BeforeEach should handle most cases for subsequent tests.
        InMemoryGameRegistry.clear();
    }

    @Test
    public void testTryStartGame_success() {
        // Arrange: Create a dummy lobby with all players ready
        Lobby lobby = new Lobby();
        Map<Long, Boolean> players = new HashMap<>();
        players.put(1L, true); // Player 1 is ready
        players.put(2L, true); // Player 2 is ready
        lobby.setPlayerReadyStatuses(players);
        lobby.setTimeLimitSeconds(60L); // Example time limit for rounds (in seconds)
        lobby.setActive(true); // Lobby must be active to start a game

        lobby = lobbyRepository.saveAndFlush(lobby);

        Game game = gameService.tryStartGame(lobby.getId());

        // Assert: Game object is returned, has an ID, and is active in the InMemoryGameRegistry
        assertNotNull(game, "The returned game object should not be null.");
        assertNotNull(game.getId(), "The game ID should not be null after creation.");
        
        assertTrue(InMemoryGameRegistry.isGameActive(game.getId()),
                "The game should be active in InMemoryGameRegistry after a successful start.");

        // Optionally, verify the GameManager instance from the registry
        GameManager activeGameManager = InMemoryGameRegistry.getGame(game.getId());
        assertNotNull(activeGameManager, "A GameManager instance should be found in the registry.");
        assertEquals(game.getId(), activeGameManager.getGameId(), "The GameManager's ID should match the Game entity's ID.");
    }

    @Test
    public void testTryStartGame_playersNotReady_shouldFail() {
        // Arrange: Create a lobby where at least one player is not ready
        Lobby lobby = new Lobby();
        Map<Long, Boolean> players = new HashMap<>();
        players.put(1L, true);  // Player 1 is ready
        players.put(2L, false); // Player 2 is NOT ready
        lobby.setPlayerReadyStatuses(players);
        lobby.setTimeLimitSeconds(60L);
        lobby.setActive(true); // The lobby itself is active
        
        lobby = lobbyRepository.saveAndFlush(lobby);

        // Act & Assert: Calling tryStartGame should throw an IllegalStateException
        Lobby finalLobby = lobby; // Effectively final variable for use in lambda
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            gameService.tryStartGame(finalLobby.getId());
        }, "tryStartGame should throw IllegalStateException if not all players are ready.");
        
    }

    @Test
    public void testGameAutoFinishesAfter10Rounds() throws InterruptedException {
        // Arrange: Create a stock timeline with 10 data points (representing 10 rounds)
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        LocalDate baseDate = LocalDate.of(2025, 4, 1);
        int numberOfRounds = 10;

        for (int i = 0; i < numberOfRounds; i++) {
            Map<String, Double> snapshot = new HashMap<>();
            snapshot.put("AAPL", 150.0 + i); // Example stock data
            timeline.put(baseDate.plusDays(i), snapshot);
        }

        Long testGameId = 999L; // A distinct ID for this test's GameManager
        int roundDurationSeconds = 1; // Use a short round duration for faster test execution (original was 5s)
        
        // Create and configure the GameManager instance directly for this test
        GameManager gameManager = new GameManager(testGameId, timeline, roundDurationSeconds);
        gameManager.registerPlayer(1L); // A game typically needs at least one player

        // Register the game in the InMemoryGameRegistry manually
        InMemoryGameRegistry.registerGame(testGameId, gameManager);
        assertTrue(InMemoryGameRegistry.isGameActive(testGameId),
                "Game should be active in the registry immediately after registration.");

        // Act: Start the game. The GameManager should handle round progression.
        gameManager.startGame();

        long processingBufferMillis = 3000; // 3-second buffer
        long totalWaitTimeMillis = ((long)numberOfRounds * roundDurationSeconds * 1000) + processingBufferMillis;
        Thread.sleep(totalWaitTimeMillis);

        assertFalse(InMemoryGameRegistry.isGameActive(testGameId),
                "Game should be removed from InMemoryGameRegistry after " + numberOfRounds + " rounds are completed. " +
                "Current round reported by GameManager: " + gameManager.getCurrentRound() +
                ". Game active in registry: " + InMemoryGameRegistry.isGameActive(testGameId));
        
        assertEquals(numberOfRounds, gameManager.getCurrentRound(),
                "GameManager should have processed all " + numberOfRounds + " rounds.");
        
    }
}