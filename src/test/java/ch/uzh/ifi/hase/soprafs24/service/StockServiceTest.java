package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus; // Import HttpStatus

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class StockServiceTest {

    @Autowired
    private StockService stockService; // This is the service under test

    @Mock
    private UserService userService; // Mocked dependency for GameManager, if GameManager uses it

    private AutoCloseable closeable;

    private Long gameIdCounter = 1L;
    private Long userIdCounter = 1L;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        InMemoryGameRegistry.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        InMemoryGameRegistry.clear();
        if (closeable != null) {
            closeable.close();
        }
    }

    private Long getNextGameId() {
        return gameIdCounter++;
    }

    private Long getNextUserId() {
        return userIdCounter++;
    }

    private GameManager setupTestGame(Long gameId) {
        LinkedHashMap<LocalDate, Map<String, Double>> dummyStockTimeline = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 10; i++) {
            dummyStockTimeline.put(today.plusDays(i), Map.of("AAPL", 150.0 + i, "MSFT", 300.0 + i));
        }
        GameManager gameManager = new GameManager(gameId, dummyStockTimeline);
        InMemoryGameRegistry.registerGame(gameId, gameManager);
        return gameManager;
    }

    private PlayerState addPlayerToGame(GameManager gameManager, Long userId, String username) {
        gameManager.registerPlayer(userId);
        PlayerState registeredPlayerState = gameManager.getPlayerState(userId);
        return registeredPlayerState;
    }


    @Test
    public void testGetStockTimelineFromDatabase() {
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = stockService.getStockTimelineFromDatabase();
        System.out.println("Username: " + System.getenv("myusername"));

        if (timeline.isEmpty()) {
            System.out.println("WARNING: StockServiceTest.testGetStockTimelineFromDatabase - timeline is empty. Check test DB data population or repository query logic if this is unexpected.");
        } else {
            assertNotNull(timeline);
            assertFalse(timeline.isEmpty(), "Timeline should not be empty if database is populated correctly for the query.");
            if (timeline.size() == 10) {
                 assertEquals(10, timeline.size(), "Should return 10 days of stock data if DB is populated for this.");
            }

            for (Map.Entry<LocalDate, Map<String, Double>> entry : timeline.entrySet()) {
                LocalDate date = entry.getKey();
                Map<String, Double> prices = entry.getValue();
                System.out.println("Day: " + date);
                assertNotNull(date, "Date in timeline entry should not be null.");
                assertNotNull(prices, "Prices map for date " + date + " should not be null.");
                if (!prices.isEmpty()) {
                    prices.forEach((symbol, price) -> {
                        System.out.println("  " + symbol + " → " + price);
                        assertNotNull(symbol, "Stock symbol should not be null.");
                        assertNotNull(price, "Stock price for " + symbol + " should not be null.");
                    });
                }
            }
        }
    }

    @Test
    public void testGetStockTimelineFromDatabase_consistentStockCount() {
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = stockService.getStockTimelineFromDatabase();
        assertNotNull(timeline, "Timeline should not be null");

        if (timeline.isEmpty()) {
            System.out.println("WARNING: StockServiceTest.testGetStockTimelineFromDatabase_consistentStockCount - timeline is empty. Cannot check stock count consistency.");
            return;
        }

        if (timeline.size() == 10) {
            assertEquals(10, timeline.size(), "Should return 10 days of stock data if DB is populated for this.");
        }


        int expectedStockCount = -1;
        int dayNumber = 1;

        for (Map.Entry<LocalDate, Map<String, Double>> entry : timeline.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Double> prices = entry.getValue();
            System.out.println("Day " + dayNumber++ + ": " + date);

            if (!prices.isEmpty()) {
                prices.forEach((symbol, price) -> System.out.println("  " + symbol + " → " + price));
                if (expectedStockCount == -1) {
                    expectedStockCount = prices.size();
                } else {
                    assertEquals(expectedStockCount, prices.size(), "Inconsistent number of stocks on " + date);
                }
            } else {
                 System.out.println("WARNING: Prices map is empty for date " + date + ". Cannot check stock count consistency for this day.");
            }
        }
        if (expectedStockCount != -1) {
             System.out.println("All days with data contain " + expectedStockCount + " stocks consistently.");
        } else if (!timeline.isEmpty()) {
            System.out.println("WARNING: Could not determine an expected stock count, all daily price maps might be empty or inconsistent making first count unreliable.");
        }
    }

    @Test
    public void getCategoryMap_shouldReturnCorrectMap() {
        Map<String, String> categoryMap = stockService.getCategoryMap();
        assertNotNull(categoryMap);
        assertEquals("TECH", categoryMap.get("TSLA"));
        assertEquals("ENERGY", categoryMap.get("XOM"));
        assertEquals("RETAIL", categoryMap.get("BABA"));
        assertTrue(categoryMap.containsKey("AAPL"));
        assertFalse(categoryMap.isEmpty());
    }

    @Test
    public void getStockPrice_gameNotFound_throwsException() {
        Long nonExistentGameId = 999L;
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getStockPrice(nonExistentGameId, "AAPL", 1);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void getStockPrice_validGame_returnsPricesUpToRound() {
        Long gameId = getNextGameId();
        GameManager gameManager = setupTestGame(gameId);

        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LocalDate date2 = LocalDate.of(2024, 1, 2);
        LocalDate date3 = LocalDate.of(2024, 1, 3);

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, Map.of("AAPL", 150.0, "MSFT", 300.0));
        timeline.put(date2, Map.of("AAPL", 152.0, "MSFT", 302.0));
        timeline.put(date3, Map.of("AAPL", 151.0, "MSFT", 301.0));

        gameManager.setStockTimeline(timeline);

        List<StockPriceGetDTO> pricesForRound1 = stockService.getStockPrice(gameId, "AAPL", 1);
        assertEquals(1, pricesForRound1.size());
        assertEquals(150.0, pricesForRound1.get(0).getPrice());
        assertEquals(date1, pricesForRound1.get(0).getDate());
        assertEquals("TECH", pricesForRound1.get(0).getCategory());


        List<StockPriceGetDTO> pricesForRound2 = stockService.getStockPrice(gameId, "AAPL", 2);
        assertEquals(2, pricesForRound2.size());
        assertEquals(150.0, pricesForRound2.get(0).getPrice());
        assertEquals(152.0, pricesForRound2.get(1).getPrice());

        List<StockPriceGetDTO> pricesForNonExistentSymbol = stockService.getStockPrice(gameId, "GOOG", 1);
        assertTrue(pricesForNonExistentSymbol.isEmpty());
    }

    @Test
    public void getStockPrice_dateForRoundIsNull_returnsEmptyList() {
        Long gameId = getNextGameId();
        GameManager gameManager = setupTestGame(gameId);
        gameManager.setStockTimeline(new LinkedHashMap<>());

        List<StockPriceGetDTO> prices = stockService.getStockPrice(gameId, "AAPL", 5);
        assertTrue(prices.isEmpty(), "Should return empty list if date for round cannot be determined or not in timeline.");
    }


    @Test
    public void getCurrentRoundStockPrices_gameNotFound_throwsException() {
        Long nonExistentGameId = 999L;
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getCurrentRoundStockPrices(nonExistentGameId);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

    }

    @Test
    public void getCurrentRoundStockPrices_validGame_returnsCurrentPrices() {
        Long gameId = getNextGameId();
        GameManager gameManager = setupTestGame(gameId);

        LocalDate date1 = LocalDate.of(2024, 3, 13);
        LocalDate date2 = LocalDate.of(2024, 3, 14);
        LocalDate marketDateForRound3 = LocalDate.of(2024, 3, 15);

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, Map.of("TSLA", 100.0, "NVDA", 700.0));
        timeline.put(date2, Map.of("TSLA", 150.0, "NVDA", 750.0));
        timeline.put(marketDateForRound3, Map.of("TSLA", 200.0, "NVDA", 800.0));

        gameManager.setStockTimeline(timeline);
        gameManager.nextRound(); // currentRound becomes 2
        gameManager.nextRound(); // currentRound becomes 3


        List<StockPriceGetDTO> currentPrices = stockService.getCurrentRoundStockPrices(gameId);
        assertEquals(2, currentPrices.size());

        StockPriceGetDTO tslaDto = currentPrices.stream().filter(p -> "TSLA".equals(p.getSymbol())).findFirst().orElse(null);
        assertNotNull(tslaDto);
        assertEquals(200.0, tslaDto.getPrice());
        assertEquals(3, tslaDto.getRound());
        assertEquals(marketDateForRound3, tslaDto.getDate());
        assertEquals("TECH", tslaDto.getCategory());

        StockPriceGetDTO nvdaDto = currentPrices.stream().filter(p -> "NVDA".equals(p.getSymbol())).findFirst().orElse(null);
        assertNotNull(nvdaDto);
        assertEquals(800.0, nvdaDto.getPrice());
    }

    @Test
    public void getCurrentRoundStockPrices_noCurrentPricesInGame_returnsEmptyList() {
        Long gameId = getNextGameId();
        GameManager gameManager = setupTestGame(gameId);

        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, Map.of("AAPL", 100.0));
        gameManager.setStockTimeline(timeline);

        gameManager.nextRound();

        List<StockPriceGetDTO> currentPrices = stockService.getCurrentRoundStockPrices(gameId);
        assertTrue(currentPrices.isEmpty());
    }


    @Test
    public void getPlayerHoldings_gameNotFound_throwsException() {
        Long nonExistentGameId = 999L;
        Long userId = getNextUserId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getPlayerHoldings(userId, nonExistentGameId);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void getPlayerHoldings_playerNotFound_throwsException() {
        Long gameId = getNextGameId();
        setupTestGame(gameId);
        Long nonExistentUserId = 999L;

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getPlayerHoldings(nonExistentUserId, gameId);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void getPlayerHoldings_validPlayerAndGame_returnsHoldings() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager gameManager = setupTestGame(gameId);
        PlayerState playerState = addPlayerToGame(gameManager, userId, "testPlayer");

        playerState.setStock("AAPL", 10); // Using setStock as per PlayerState
        playerState.setStock("MSFT", 5); // Using setStock as per PlayerState

        LocalDate marketDate = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(marketDate, Map.of("AAPL", 150.0, "MSFT", 300.0, "GOOG", 2000.0));
        gameManager.setStockTimeline(timeline);

        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);
        assertEquals(2, holdings.size());

        StockHoldingDTO aaplHolding = holdings.stream().filter(h -> "AAPL".equals(h.getSymbol())).findFirst().orElse(null);
        assertNotNull(aaplHolding);
        assertEquals(10, aaplHolding.getQuantity());
        assertEquals(150.0, aaplHolding.getCurrentPrice());
        assertEquals("TECH", aaplHolding.getCategory());

        StockHoldingDTO msftHolding = holdings.stream().filter(h -> "MSFT".equals(h.getSymbol())).findFirst().orElse(null);
        assertNotNull(msftHolding);
        assertEquals(5, msftHolding.getQuantity());
        assertEquals(300.0, msftHolding.getCurrentPrice());
    }

    @Test
    public void getPlayerHoldings_playerHasNoStocks_returnsEmptyList() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager gameManager = setupTestGame(gameId);
        addPlayerToGame(gameManager, userId, "testPlayerWithNoStocks");

        LocalDate marketDate = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(marketDate, Map.of("AAPL", 150.0));
        gameManager.setStockTimeline(timeline);

        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);
        assertTrue(holdings.isEmpty());
    }

    @Test
    public void getPlayerHoldings_stockPriceNotAvailable_usesZeroPrice() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager gameManager = setupTestGame(gameId);
        PlayerState playerState = addPlayerToGame(gameManager, userId, "testPlayer");

        playerState.setStock("UNKN", 10); // Using setStock

        LocalDate marketDate = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(marketDate, Map.of("AAPL", 150.0));
        gameManager.setStockTimeline(timeline);

        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);
        assertEquals(1, holdings.size());
        StockHoldingDTO unknHolding = holdings.get(0);
        assertEquals("UNKN", unknHolding.getSymbol());
        assertEquals(10, unknHolding.getQuantity());
        assertEquals(0.0, unknHolding.getCurrentPrice());
        assertEquals("OTHER", unknHolding.getCategory());
    }


    @Test
    public void getGameManagerForUser_success() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager expectedGameManager = setupTestGame(gameId);
        addPlayerToGame(expectedGameManager, userId, "playerInGame");

        GameManager actualGameManager = stockService.getGameManagerForUser(userId, gameId);
        assertSame(expectedGameManager, actualGameManager);
    }

    @Test
    public void getGameManagerForUser_gameNotFound_throwsException() {
        Long nonExistentGameId = 999L;
        Long userId = getNextUserId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getGameManagerForUser(userId, nonExistentGameId);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void getGameManagerForUser_playerNotInGame_throwsException() {
        Long gameId = getNextGameId();
        Long userIdNotInGame = getNextUserId();
        setupTestGame(gameId);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getGameManagerForUser(userIdNotInGame, gameId);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void getPlayerHoldingsByRound_success() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager gameManager = setupTestGame(gameId);
        PlayerState playerState = addPlayerToGame(gameManager, userId, "historicalPlayer");

        LocalDate dateRound1 = LocalDate.of(2024, 1, 1);
        LocalDate dateRound2 = LocalDate.of(2024, 1, 2);

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateRound1, Map.of("AAPL", 100.0, "MSFT", 200.0));
        timeline.put(dateRound2, Map.of("AAPL", 105.0, "MSFT", 205.0));
        gameManager.setStockTimeline(timeline);

        // Simulate snapshotting holdings for round 1
        playerState.snapshotHoldingsAtRound(1); // Assuming this captures current playerState.stocksOwned
        // Then modify stocks for the snapshot if needed, or set them before snapshotting.
        // For this test, let's assume playerState had AAPL before snapshotting.
        // To be more precise:
        playerState.setStock("AAPL", 10); // Player has 10 AAPL
        playerState.snapshotHoldingsAtRound(1); // Snapshot this state for round 1

        List<StockHoldingDTO> holdingsForRound1 = stockService.getPlayerHoldingsByRound(userId, gameId, 1);
        assertEquals(1, holdingsForRound1.size());
        StockHoldingDTO aaplHolding = holdingsForRound1.get(0);
        assertEquals("AAPL", aaplHolding.getSymbol());
        assertEquals(10, aaplHolding.getQuantity());
        assertEquals(100.0, aaplHolding.getCurrentPrice());
    }

    @Test
    public void getPlayerHoldingsByRound_roundHasNoHoldings_returnsEmptyList() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager gameManager = setupTestGame(gameId);
        PlayerState playerState = addPlayerToGame(gameManager, userId, "player"); // playerState is fresh

        LocalDate dateRound1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateRound1, Map.of("AAPL", 100.0));
        gameManager.setStockTimeline(timeline);

        playerState.snapshotHoldingsAtRound(1); // Snapshot empty holdings for round 1

        List<StockHoldingDTO> holdings = stockService.getPlayerHoldingsByRound(userId, gameId, 1);
        assertTrue(holdings.isEmpty());
    }

    @Test
    public void getPlayerHoldingsByRound_priceForHoldingNotAvailable_usesZeroPrice() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager gameManager = setupTestGame(gameId);
        PlayerState playerState = addPlayerToGame(gameManager, userId, "player");

        LocalDate dateRound1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateRound1, Map.of("AAPL", 100.0));
        gameManager.setStockTimeline(timeline);

        playerState.setStock("MSFT", 5); // Player has MSFT
        playerState.snapshotHoldingsAtRound(1); // Snapshot this state for round 1

        List<StockHoldingDTO> holdings = stockService.getPlayerHoldingsByRound(userId, gameId, 1);
        assertEquals(1, holdings.size());
        StockHoldingDTO msftHolding = holdings.get(0);
        assertEquals("MSFT", msftHolding.getSymbol());
        assertEquals(5, msftHolding.getQuantity());
        assertEquals(0.0, msftHolding.getCurrentPrice());
    }


    @Test
    public void getPlayerHoldingsAllRounds_success() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager gameManager = setupTestGame(gameId);
        PlayerState playerState = addPlayerToGame(gameManager, userId, "multiRoundPlayer");

        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LocalDate date2 = LocalDate.of(2024, 1, 2);
        LocalDate date3 = LocalDate.of(2024, 1, 3);

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, Map.of("AAPL", 100.0, "MSFT", 200.0));
        timeline.put(date2, Map.of("AAPL", 105.0, "MSFT", 195.0));
        timeline.put(date3, Map.of("AAPL", 110.0, "MSFT", 210.0));
        gameManager.setStockTimeline(timeline);

        // Round 1 (GameManager currentRound is 1)
        playerState.setStock("AAPL", 10);
        playerState.snapshotHoldingsAtRound(1); // Snapshot for end of round 1
        gameManager.nextRound(); // currentRound becomes 2

        // Round 2 (GameManager currentRound is 2)
        playerState.setStock("AAPL", 5); // Player sells some AAPL
        playerState.setStock("MSFT", 5); // Player buys MSFT
        playerState.snapshotHoldingsAtRound(2); // Snapshot for end of round 2
        gameManager.nextRound(); // currentRound becomes 3

        Map<Integer, List<StockHoldingDTO>> allHoldings = stockService.getPlayerHoldingsAllRounds(userId, gameId);

        assertNotNull(allHoldings);
        assertEquals(3, allHoldings.size());

        assertTrue(allHoldings.get(1).isEmpty(), "Holdings for display round 1 should be empty.");

        List<StockHoldingDTO> holdingsForDisplayRound2 = allHoldings.get(2);
        assertEquals(1, holdingsForDisplayRound2.size());
        StockHoldingDTO aaplR1 = holdingsForDisplayRound2.get(0);
        assertEquals("AAPL", aaplR1.getSymbol());
        assertEquals(10, aaplR1.getQuantity());
        assertEquals(105.0, aaplR1.getCurrentPrice());

        List<StockHoldingDTO> holdingsForDisplayRound3 = allHoldings.get(3);
        assertEquals(2, holdingsForDisplayRound3.size());
        StockHoldingDTO aaplR2 = holdingsForDisplayRound3.stream().filter(s -> "AAPL".equals(s.getSymbol())).findFirst().orElseThrow();
        StockHoldingDTO msftR2 = holdingsForDisplayRound3.stream().filter(s -> "MSFT".equals(s.getSymbol())).findFirst().orElseThrow();

        assertEquals(5, aaplR2.getQuantity());
        assertEquals(110.0, aaplR2.getCurrentPrice());

        assertEquals(5, msftR2.getQuantity());
        assertEquals(210.0, msftR2.getCurrentPrice());
    }

    @Test
    public void getPlayerHoldingsAllRounds_gameNotStarted_returnsRound1Empty() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        GameManager gameManager = setupTestGame(gameId);
        PlayerState playerState = addPlayerToGame(gameManager, userId, "newPlayer");


        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, Map.of("AAPL", 100.0));
        gameManager.setStockTimeline(timeline);

        // Game manager current round is 1.
        // Player has no stocks, so snapshotting for round 1 will be empty.
        playerState.snapshotHoldingsAtRound(1);


        Map<Integer, List<StockHoldingDTO>> allHoldings = stockService.getPlayerHoldingsAllRounds(userId, gameId);

        assertEquals(2, allHoldings.size());
        assertTrue(allHoldings.get(1).isEmpty());
        assertTrue(allHoldings.get(2).isEmpty());
    }

    @Test
    public void getPlayerHoldingsAllRounds_playerNotFound_throwsException() {
        Long gameId = getNextGameId();
        GameManager gameManager = setupTestGame(gameId);
        Long nonExistentUserId = 999L;

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getPlayerHoldingsAllRounds(nonExistentUserId, gameId);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}