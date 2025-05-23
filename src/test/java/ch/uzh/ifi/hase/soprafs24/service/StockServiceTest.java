package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;

@ExtendWith(MockitoExtension.class)
public class StockServiceTest {

    @InjectMocks
    private StockService stockService;
    private Long gameIdCounter;
    private Long userIdCounter;

    @BeforeEach
    void setUp() {
        InMemoryGameRegistry.clear();
        gameIdCounter = 1L;
        userIdCounter = 1L;
    }

    @AfterEach
    void tearDown() {
        InMemoryGameRegistry.clear();
    }

    private Long getNextGameId() {
        return gameIdCounter++;
    }

    private Long getNextUserId() {
        return userIdCounter++;
    }

    private GameManager setupActualGameInRegistry(Long gameId, LinkedHashMap<LocalDate, Map<String, Double>> timeline) {
        GameManager gameManager = new GameManager(gameId, timeline);
        InMemoryGameRegistry.registerGame(gameId, gameManager);
        return gameManager;
    }

    private PlayerState addPlayerToActualGame(GameManager gameManager, Long userId) {
        gameManager.registerPlayer(userId);
        return gameManager.getPlayerState(userId);
    }

    @Test
    public void getStockTimelineFromDatabase_should_be_tested_via_integration_test_or_mock_repo() {
        System.out.println(
                "Unit test for getStockTimelineFromDatabase: This test is skipped in this unit test setup as it implies database interaction not covered by mocking InMemoryGameRegistry.");
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
        String actualReason = exception.getReason();
        String expectedMessage = "Game with ID " + nonExistentGameId + " not found.";
        assertTrue(actualReason != null && actualReason.equals(expectedMessage),
                "Exception reason should be '" + expectedMessage + "'. Actual: " + actualReason);
    }

    @Test
    public void getStockPrice_validGame_returnsPricesUpToRound() {
        Long gameId = getNextGameId();
        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LocalDate date2 = LocalDate.of(2024, 1, 2);
        LocalDate date3 = LocalDate.of(2024, 1, 3);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, Map.of("AAPL", 150.0, "MSFT", 300.0));
        timeline.put(date2, Map.of("AAPL", 152.0, "MSFT", 302.0));
        timeline.put(date3, Map.of("AAPL", 151.0, "MSFT", 301.0));
        setupActualGameInRegistry(gameId, timeline);
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
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.of(2024, 1, 1), Map.of("AAPL", 100.0));
        setupActualGameInRegistry(gameId, timeline);
        List<StockPriceGetDTO> prices = stockService.getStockPrice(gameId, "AAPL", 0);
        assertTrue(prices.isEmpty());
        List<StockPriceGetDTO> pricesHighRound = stockService.getStockPrice(gameId, "AAPL", 5);
        assertTrue(pricesHighRound.isEmpty());
    }

    @Test
    public void getStockPrice_emptyTimeline_returnsEmptyList() {
        Long gameId = getNextGameId();
        setupActualGameInRegistry(gameId, new LinkedHashMap<>());

        List<StockPriceGetDTO> prices = stockService.getStockPrice(gameId, "AAPL", 1);
        assertTrue(prices.isEmpty());
    }

    @Test
    public void getStockPrice_unknownSymbolCategory_returnsOtherCategory() {
        Long gameId = getNextGameId();
        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, Map.of("UNKNOWN", 50.0));
        setupActualGameInRegistry(gameId, timeline);

        List<StockPriceGetDTO> prices = stockService.getStockPrice(gameId, "UNKNOWN", 1);
        assertEquals(1, prices.size());
        assertEquals("OTHER", prices.get(0).getCategory());
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
        LocalDate date1 = LocalDate.of(2024, 3, 13);
        Map<String, Double> round1Prices = Map.of("TSLA", 200.0, "NVDA", 800.0);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, round1Prices);

        setupActualGameInRegistry(gameId, timeline);

        List<StockPriceGetDTO> currentPrices = stockService.getCurrentRoundStockPrices(gameId);
        assertEquals(2, currentPrices.size());

        StockPriceGetDTO tslaDto = currentPrices.stream().filter(p -> "TSLA".equals(p.getSymbol())).findFirst()
                .orElse(null);
        assertNotNull(tslaDto);
        assertEquals(200.0, tslaDto.getPrice());
        assertEquals(1, tslaDto.getRound());
        assertEquals(date1, tslaDto.getDate());
        assertEquals("TECH", tslaDto.getCategory());

        StockPriceGetDTO nvdaDto = currentPrices.stream().filter(p -> "NVDA".equals(p.getSymbol())).findFirst()
                .orElse(null);
        assertNotNull(nvdaDto);
        assertEquals(800.0, nvdaDto.getPrice());
    }

    @Test
    public void getCurrentRoundStockPrices_noCurrentPricesInGameManager_returnsEmptyList() {
        Long gameId = getNextGameId();
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.of(2024, 1, 1), Collections.emptyMap());
        setupActualGameInRegistry(gameId, timeline);

        List<StockPriceGetDTO> currentPrices = stockService.getCurrentRoundStockPrices(gameId);
        assertTrue(currentPrices.isEmpty());
    }

    @Test
    public void getCurrentRoundStockPrices_unknownSymbolCategory_returnsOtherCategory() {
        Long gameId = getNextGameId();
        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(date1, Map.of("UNKNOWN", 50.0));
        setupActualGameInRegistry(gameId, timeline);

        List<StockPriceGetDTO> currentPrices = stockService.getCurrentRoundStockPrices(gameId);
        assertEquals(1, currentPrices.size());
        assertEquals("OTHER", currentPrices.get(0).getCategory());
        assertEquals(50.0, currentPrices.get(0).getPrice());
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
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.now(), Collections.singletonMap("AAPL", 100.0));
        setupActualGameInRegistry(gameId, timeline);

        Long nonExistentUserId = getNextUserId();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getPlayerHoldings(nonExistentUserId, gameId);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        String actualReason = exception.getReason();
        String expectedReason = "Player state not found for userId: " + nonExistentUserId;
        assertTrue(actualReason != null && actualReason.equals(expectedReason),
                "Exception reason should be '" + expectedReason + "'. Actual: " + actualReason);
    }

    @Test
    public void getPlayerHoldings_validPlayerAndGame_returnsHoldings() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();

        LocalDate marketDate = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(marketDate, Map.of("AAPL", 150.0, "MSFT", 300.0, "GOOG", 2000.0));
        GameManager gameManager = setupActualGameInRegistry(gameId, timeline);
        PlayerState playerState = addPlayerToActualGame(gameManager, userId);

        playerState.setStock("AAPL", 10);
        playerState.setStock("MSFT", 5);

        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);
        assertEquals(2, holdings.size());

        StockHoldingDTO aaplHolding = holdings.stream().filter(h -> "AAPL".equals(h.getSymbol())).findFirst()
                .orElse(null);
        assertNotNull(aaplHolding);
        assertEquals(10, aaplHolding.getQuantity());
        assertEquals(150.0, aaplHolding.getCurrentPrice());
        assertEquals("TECH", aaplHolding.getCategory());

        StockHoldingDTO msftHolding = holdings.stream().filter(h -> "MSFT".equals(h.getSymbol())).findFirst()
                .orElse(null);
        assertNotNull(msftHolding);
        assertEquals(5, msftHolding.getQuantity());
        assertEquals(300.0, msftHolding.getCurrentPrice());
    }

    @Test
    public void getPlayerHoldings_playerHasNoStocks_returnsEmptyList() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.now(), Map.of("AAPL", 100.0));
        GameManager gameManager = setupActualGameInRegistry(gameId, timeline);
        addPlayerToActualGame(gameManager, userId);

        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);
        assertTrue(holdings.isEmpty());
    }

    @Test
    public void getPlayerHoldings_playerStockQuantityZeroOrNegative_isSkipped() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.now(), Map.of("AAPL", 100.0, "MSFT", 200.0, "GOOG", 300.0));
        GameManager gameManager = setupActualGameInRegistry(gameId, timeline);
        PlayerState playerState = addPlayerToActualGame(gameManager, userId);
        playerState.setStock("AAPL", 0);
        playerState.setStock("MSFT", -5);
        playerState.setStock("GOOG", 10);
        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);
        assertEquals(1, holdings.size());
        assertEquals("GOOG", holdings.get(0).getSymbol());
        assertEquals(10, holdings.get(0).getQuantity());
    }

    @Test
    public void getPlayerHoldings_unknownSymbolCategory_returnsOtherCategory() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.now(), Map.of("UNKNOWN", 50.0));
        GameManager gameManager = setupActualGameInRegistry(gameId, timeline);
        PlayerState playerState = addPlayerToActualGame(gameManager, userId);
        playerState.setStock("UNKNOWN", 5);
        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);
        assertEquals(1, holdings.size());
        assertEquals("OTHER", holdings.get(0).getCategory());
        assertEquals(50.0, holdings.get(0).getCurrentPrice());
    }

    @Test
    public void getGameManagerForUser_success() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.now(), Collections.singletonMap("AAPL", 100.0));
        GameManager expectedGameManager = setupActualGameInRegistry(gameId, timeline);
        addPlayerToActualGame(expectedGameManager, userId);

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
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.now(), Collections.singletonMap("AAPL", 100.0));
        setupActualGameInRegistry(gameId, timeline);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            stockService.getGameManagerForUser(userIdNotInGame, gameId);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        String actualReason = exception.getReason();
        String expectedReason = "Player " + userIdNotInGame + " not found in game " + gameId;
        assertTrue(actualReason != null && actualReason.equals(expectedReason),
                "Exception reason should be '" + expectedReason + "'. Actual: " + actualReason);
    }

    @Test
    public void getPlayerHoldingsByRound_gameNotFound_throwsException() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stockService.getPlayerHoldingsByRound(userId, gameId, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getReason().contains("Game not found"));
    }

    @Test
    public void getPlayerHoldingsByRound_playerNotFound_throwsException() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.now(), Map.of("AAPL", 100.0));
        setupActualGameInRegistry(gameId, timeline);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stockService.getPlayerHoldingsByRound(userId, gameId, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getReason().contains("Player not found"));
    }

    @Test
    public void getPlayerHoldingsByRound_validData_returnsHoldings() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LocalDate dateR1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1, Map.of("AAPL", 100.0, "MSFT", 200.0));
        GameManager game = setupActualGameInRegistry(gameId, timeline);
        PlayerState player = addPlayerToActualGame(game, userId);
        player.setStock("AAPL", 10);
        player.setStock("MSFT", 5);
        player.snapshotHoldingsAtRound(1);
        List<StockHoldingDTO> result = stockService.getPlayerHoldingsByRound(userId, gameId, 1);
        assertEquals(2, result.size());
        StockHoldingDTO aapl = result.stream().filter(s -> "AAPL".equals(s.getSymbol())).findFirst().orElseThrow();
        assertEquals(10, aapl.getQuantity());
        assertEquals(100.0, aapl.getCurrentPrice());
        assertEquals("TECH", aapl.getCategory());
        StockHoldingDTO msft = result.stream().filter(s -> "MSFT".equals(s.getSymbol())).findFirst().orElseThrow();
        assertEquals(5, msft.getQuantity());
        assertEquals(200.0, msft.getCurrentPrice());
        assertEquals("TECH", msft.getCategory());
    }

    @Test
    public void getPlayerHoldingsByRound_emptySnapshot_returnsEmptyList() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LocalDate dateR1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1, Map.of("AAPL", 100.0));
        GameManager game = setupActualGameInRegistry(gameId, timeline);
        PlayerState player = addPlayerToActualGame(game, userId);
        player.snapshotHoldingsAtRound(1);
        List<StockHoldingDTO> result = stockService.getPlayerHoldingsByRound(userId, gameId, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getPlayerHoldingsByRound_pricesForRoundDateEmpty_returnsHoldingsWithZeroPrice() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LocalDate dateR1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1, Collections.emptyMap());
        GameManager game = setupActualGameInRegistry(gameId, timeline);
        PlayerState player = addPlayerToActualGame(game, userId);
        player.setStock("AAPL", 10);
        player.snapshotHoldingsAtRound(1);
        List<StockHoldingDTO> result = stockService.getPlayerHoldingsByRound(userId, gameId, 1);
        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).getCurrentPrice());
    }

    @Test
    public void getPlayerHoldingsByRound_stockWithZeroQuantity_isSkipped() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LocalDate dateR1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1, Map.of("AAPL", 100.0, "MSFT", 200.0));
        GameManager game = setupActualGameInRegistry(gameId, timeline);
        PlayerState player = addPlayerToActualGame(game, userId);
        player.setStock("AAPL", 0);
        player.setStock("MSFT", 5);
        player.snapshotHoldingsAtRound(1);
        List<StockHoldingDTO> result = stockService.getPlayerHoldingsByRound(userId, gameId, 1);
        assertEquals(1, result.size());
        assertEquals("MSFT", result.get(0).getSymbol());
    }

    @Test
    public void getPlayerHoldingsByRound_unknownSymbolCategory_returnsOtherCategory() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LocalDate dateR1 = LocalDate.of(2024, 1, 1);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1, Map.of("UNKNOWN", 50.0));
        GameManager game = setupActualGameInRegistry(gameId, timeline);
        PlayerState player = addPlayerToActualGame(game, userId);
        player.setStock("UNKNOWN", 10);
        player.snapshotHoldingsAtRound(1);
        List<StockHoldingDTO> result = stockService.getPlayerHoldingsByRound(userId, gameId, 1);
        assertEquals(1, result.size());
        assertEquals("OTHER", result.get(0).getCategory());
        assertEquals(50.0, result.get(0).getCurrentPrice());
    }

    @Test
    public void getPlayerHoldingsAllRounds_success() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LocalDate dateR1Market = LocalDate.of(2024, 1, 1);
        LocalDate dateR2Market = LocalDate.of(2024, 1, 2);
        LocalDate dateR3Market = LocalDate.of(2024, 1, 3);
        LocalDate dateR4Market = LocalDate.of(2024, 1, 4);
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1Market, Map.of("AAPL", 100.0, "MSFT", 200.0));
        timeline.put(dateR2Market, Map.of("AAPL", 105.0, "MSFT", 205.0));
        timeline.put(dateR3Market, Map.of("AAPL", 110.0, "MSFT", 210.0));
        timeline.put(dateR4Market, Map.of("AAPL", 115.0, "MSFT", 215.0));
        GameManager gameManager = new GameManager(gameId, timeline, 100L);
        InMemoryGameRegistry.registerGame(gameId, gameManager);
        PlayerState playerState = addPlayerToActualGame(gameManager, userId);
        playerState.setStock("AAPL", 10);
        playerState.snapshotHoldingsAtRound(1);
        playerState.markSubmittedForRound(1);
        gameManager.nextRound();
        playerState.setStock("AAPL", 5);
        playerState.setStock("MSFT", 5);
        playerState.snapshotHoldingsAtRound(2);
        playerState.markSubmittedForRound(2);
        gameManager.nextRound();
        playerState.snapshotHoldingsAtRound(3);
        playerState.markSubmittedForRound(3);

        Map<Integer, List<StockHoldingDTO>> allHoldings = stockService.getPlayerHoldingsAllRounds(userId, gameId);

        assertNotNull(allHoldings);
        assertEquals(4, allHoldings.size(), "Map keys: 1,2,3,4.");

        assertTrue(allHoldings.containsKey(1));
        List<StockHoldingDTO> holdingsDispR1 = allHoldings.get(1);
        assertNotNull(holdingsDispR1);
        assertTrue(holdingsDispR1.isEmpty(), "Display Round 1 should be empty (initial state)");

        assertTrue(allHoldings.containsKey(2));
        List<StockHoldingDTO> holdingsDispR2 = allHoldings.get(2);
        assertNotNull(holdingsDispR2);
        assertEquals(1, holdingsDispR2.size());
        StockHoldingDTO aaplDispR2 = holdingsDispR2.stream().filter(s -> "AAPL".equals(s.getSymbol())).findFirst()
                .orElseThrow();
        assertEquals("AAPL", aaplDispR2.getSymbol());
        assertEquals(10, aaplDispR2.getQuantity());
        assertEquals(105.0, aaplDispR2.getCurrentPrice(), "AAPL price from dateR2Market for Display Round 2");

        assertTrue(allHoldings.containsKey(3));
        List<StockHoldingDTO> holdingsDispR3 = allHoldings.get(3);
        assertNotNull(holdingsDispR3);
        assertEquals(2, holdingsDispR3.size());
        StockHoldingDTO aaplDispR3 = holdingsDispR3.stream().filter(s -> "AAPL".equals(s.getSymbol())).findFirst()
                .orElseThrow();
        assertEquals(5, aaplDispR3.getQuantity());
        assertEquals(110.0, aaplDispR3.getCurrentPrice(), "AAPL price from dateR3Market for Display Round 3");
        StockHoldingDTO msftDispR3 = holdingsDispR3.stream().filter(s -> "MSFT".equals(s.getSymbol())).findFirst()
                .orElseThrow();
        assertEquals(5, msftDispR3.getQuantity());
        assertEquals(210.0, msftDispR3.getCurrentPrice(), "MSFT price from dateR3Market for Display Round 3");

        assertTrue(allHoldings.containsKey(4));
        List<StockHoldingDTO> holdingsDispR4 = allHoldings.get(4);
        assertNotNull(holdingsDispR4);
        assertEquals(2, holdingsDispR4.size());
        StockHoldingDTO aaplDispR4 = holdingsDispR4.stream().filter(s -> "AAPL".equals(s.getSymbol())).findFirst()
                .orElseThrow();
        assertEquals(5, aaplDispR4.getQuantity());
        assertEquals(115.0, aaplDispR4.getCurrentPrice(), "AAPL price from dateR4Market for Display Round 4");
        StockHoldingDTO msftDispR4 = holdingsDispR4.stream().filter(s -> "MSFT".equals(s.getSymbol())).findFirst()
                .orElseThrow();
        assertEquals(5, msftDispR4.getQuantity());
        assertEquals(215.0, msftDispR4.getCurrentPrice(), "MSFT price from dateR4Market for Display Round 4");

        gameManager.endGame();
    }

    @Test
    public void getPlayerHoldingsAllRounds_gameNotFound_throwsException() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stockService.getPlayerHoldingsAllRounds(userId, gameId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getReason().contains("Game not found"));
    }

    @Test
    public void getPlayerHoldingsAllRounds_playerNotFound_throwsException() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(LocalDate.now(), Map.of("AAPL", 100.0));
        setupActualGameInRegistry(gameId, timeline);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stockService.getPlayerHoldingsAllRounds(userId, gameId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getReason().contains("Player not found"));
    }

    @Test
    public void getPlayerHoldingsAllRounds_gameAtInitialRound_returnsInitialAndOneCalculatedRound() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();

        LocalDate dateR1Market = LocalDate.of(2024, 1, 1);
        LocalDate dateR2Market = LocalDate.of(2024, 1, 2);

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1Market, Map.of("AAPL", 100.0));
        timeline.put(dateR2Market, Map.of("AAPL", 105.0));

        GameManager gameManager = new GameManager(gameId, timeline, 100L);
        InMemoryGameRegistry.registerGame(gameId, gameManager);
        PlayerState playerState = addPlayerToActualGame(gameManager, userId);

        playerState.setStock("AAPL", 5);
        playerState.snapshotHoldingsAtRound(1);

        Map<Integer, List<StockHoldingDTO>> allHoldings = stockService.getPlayerHoldingsAllRounds(userId, gameId);

        assertNotNull(allHoldings);
        assertEquals(2, allHoldings.size(),
                "Should have initial (Display R1) and holdings after actual R1 (Display R2). Map keys: 1,2");

        assertTrue(allHoldings.containsKey(1));
        assertTrue(allHoldings.get(1).isEmpty(), "Display Round 1 is initial empty.");

        assertTrue(allHoldings.containsKey(2));
        List<StockHoldingDTO> holdingsDispR2 = allHoldings.get(2);
        assertEquals(1, holdingsDispR2.size());
        assertEquals("AAPL", holdingsDispR2.get(0).getSymbol());
        assertEquals(5, holdingsDispR2.get(0).getQuantity());
        assertEquals(105.0, holdingsDispR2.get(0).getCurrentPrice(),
                "AAPL price from dateR2Market for Display Round 2");

        gameManager.endGame();
    }

    @Test
    public void getPlayerHoldingsAllRounds_priceDateForDisplayRoundMissing_priceIsZero() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();

        LocalDate dateR1Market = LocalDate.of(2024, 1, 1);

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1Market, Map.of("AAPL", 100.0));

        GameManager gameManager = new GameManager(gameId, timeline, 100L);
        InMemoryGameRegistry.registerGame(gameId, gameManager);
        PlayerState playerState = addPlayerToActualGame(gameManager, userId);

        playerState.setStock("AAPL", 5);
        playerState.snapshotHoldingsAtRound(1);

        Map<Integer, List<StockHoldingDTO>> allHoldings = stockService.getPlayerHoldingsAllRounds(userId, gameId);

        assertEquals(2, allHoldings.size());
        List<StockHoldingDTO> holdingsDispR2 = allHoldings.get(2);
        assertNotNull(holdingsDispR2);
        assertEquals(1, holdingsDispR2.size());
        assertEquals("AAPL", holdingsDispR2.get(0).getSymbol());
        assertEquals(5, holdingsDispR2.get(0).getQuantity());
        assertEquals(0.0, holdingsDispR2.get(0).getCurrentPrice(),
                "Price should be 0.0 as dateR2Market data is missing");

        gameManager.endGame();
    }

    @Test
    public void getPlayerHoldingsAllRounds_unknownSymbolCategory_returnsOtherCategory() {
        Long gameId = getNextGameId();
        Long userId = getNextUserId();

        LocalDate dateR1Market = LocalDate.of(2024, 1, 1);
        LocalDate dateR2Market = LocalDate.of(2024, 1, 2);

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        timeline.put(dateR1Market, Map.of("UNKNOWN", 50.0));
        timeline.put(dateR2Market, Map.of("UNKNOWN", 55.0));

        GameManager gameManager = new GameManager(gameId, timeline, 100L);
        InMemoryGameRegistry.registerGame(gameId, gameManager);
        PlayerState playerState = addPlayerToActualGame(gameManager, userId);

        playerState.setStock("UNKNOWN", 3);
        playerState.snapshotHoldingsAtRound(1);

        Map<Integer, List<StockHoldingDTO>> allHoldings = stockService.getPlayerHoldingsAllRounds(userId, gameId);

        List<StockHoldingDTO> holdingsDispR2 = allHoldings.get(2);
        assertNotNull(holdingsDispR2);
        assertEquals(1, holdingsDispR2.size());
        StockHoldingDTO unknownHolding = holdingsDispR2.get(0);
        assertEquals("UNKNOWN", unknownHolding.getSymbol());
        assertEquals("OTHER", unknownHolding.getCategory());
        assertEquals(55.0, unknownHolding.getCurrentPrice());

        gameManager.endGame();
    }
}