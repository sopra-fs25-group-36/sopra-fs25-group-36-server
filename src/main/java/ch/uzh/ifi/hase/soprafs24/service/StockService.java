package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
//import org.checkerframework.checker.units.qual.A;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.crazzyghost.alphavantage.AlphaVantage;
import com.crazzyghost.alphavantage.Config;
import com.crazzyghost.alphavantage.parameters.DataType;
import com.crazzyghost.alphavantage.parameters.OutputSize;
import com.crazzyghost.alphavantage.timeseries.response.TimeSeriesResponse;

import ch.uzh.ifi.hase.soprafs24.entity.Stock;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.repository.StockRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository, @Value("${ALPHAVANTAGE_API_KEY}") String API_KEY) {
        this.stockRepository = stockRepository;
        this.API_KEY = API_KEY;
    }

    private String API_KEY;

    // for updating more data from dbs_April.13
    private static final List<String> POPULAR_SYMBOLS = List.of(
            "TSLA", "GOOG", "MSFT", "NVDA", "AMZN", "META", "NFLX", "INTC", "AMD", "AAPL",
            "JPM", "GS",
            "PFE", "JNJ",
            "XOM", "CVX",
            "PG");

    private static final Map<String, String> STOCK_CATEGORIES = Map.ofEntries(
            Map.entry("TSLA", "TECH"), Map.entry("GOOG", "TECH"),
            Map.entry("MSFT", "TECH"), Map.entry("NVDA", "TECH"),
            Map.entry("AMZN", "TECH"), Map.entry("META", "TECH"),
            Map.entry("NFLX", "TECH"), Map.entry("INTC", "TECH"),
            Map.entry("AMD", "TECH"), Map.entry("AAPL", "TECH"),
            Map.entry("XOM", "ENERGY"), Map.entry("CVX", "ENERGY"),
            Map.entry("JPM", "FINANCE"), Map.entry("GS", "FINANCE"),
            Map.entry("PFE", "HEALTHCARE"), Map.entry("JNJ", "HEALTHCARE"),
            Map.entry("PG", "CONSUMER"));

    public Map<String, String> getCategoryMap() {
        return STOCK_CATEGORIES;

    }

 
    @Scheduled(cron = "0 0 1 * * ?") // everyday 1am! i change it like this as if i do it based on millisecond everytime i run the backend it refetch this function automatically
    public void scheduleStockUpdate() {
        System.out.println("Starting scheduled stock data fetch at 1 AM! :)))!");
        fetchKnownPopularStocks();
        System.out.println("Finished scheduled stock data fetch!!:))))!");
    }

    public void fetchKnownPopularStocks() {
        for (String symbol : POPULAR_SYMBOLS) {
            try {
                // fetchAndProcessStockData(symbol); //UNCOMMENT
            } catch (Exception e) {
                System.err.println("Failed to fetch data for " + symbol + ": " + e.getMessage());
            }
        }
    }

    // 
    public void fetchAndProcessStockData(String symbol) {
        Config cfg = Config.builder()
                .key(API_KEY)
                .timeOut(10)
                .build();

        AlphaVantage.api().init(cfg);

        TimeSeriesResponse response = AlphaVantage.api()
                .timeSeries()
                .daily()
                .forSymbol(symbol)
                .outputSize(OutputSize.FULL)
                .dataType(DataType.JSON)
                .fetchSync();

        System.out.println("Metadata: " + response.getMetaData().getInformation());
        System.out.println("Error message: " + response.getErrorMessage());

        response.getStockUnits().forEach(stockUnit -> {
            System.out.println("Date: " + stockUnit.getDate());
            System.out.println("Open: " + stockUnit.getOpen());
            System.out.println("High: " + stockUnit.getHigh());
            System.out.println("Low: " + stockUnit.getLow());
            System.out.println("Close: " + stockUnit.getClose());
            System.out.println("Volume: " + stockUnit.getVolume());

            if (stockRepository
                    .findBySymbolAndDate(symbol, LocalDate.parse(stockUnit.getDate(), DateTimeFormatter.ISO_DATE))
                    .size() == 0) {
                Stock stock = new Stock();
                stock.setDate(LocalDate.parse(stockUnit.getDate(), DateTimeFormatter.ISO_DATE));
                stock.setPrice(stockUnit.getClose());
                stock.setCurrency("USD");
                stock.setVolume(stockUnit.getVolume());
                stock.setSymbol(symbol);
                stockRepository.save(stock);
                stockRepository.flush();
            }
        });

        System.out.println("Saved " + response.getStockUnits().size() + " records for " + symbol);
    }

    public List<StockPriceGetDTO> getStockPrice(Long gameId, String symbol, Integer round) {
        GameManager game = InMemoryGameRegistry.getGame(gameId);

        if (game == null) {
            throw new IllegalArgumentException("Game with ID " + gameId + " not found.");
        }

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = game.getStockTimeline();

        return timeline.entrySet().stream()
                .map(entry -> {
                    StockPriceGetDTO dto = new StockPriceGetDTO();
                    dto.setSymbol(symbol);
                    dto.setDate(entry.getKey()); // make sure StockPriceGetDTO has `date`
                    dto.setPrice(entry.getValue().get(symbol));
                    return dto;
                })
                .toList();
    }

    // CREATE STOCK TIMELINE UNIQUE TO GAME ; EXTRACTING STOCKS FROM DB TO GAME
    public LinkedHashMap<LocalDate, Map<String, Double>> getStockTimelineFromDatabase() {
        LinkedHashMap<LocalDate, Map<String, Double>> byDate = new LinkedHashMap<>();
        LocalDate startDate = stockRepository.findRandomStartDateWith10Days();

        List<Stock> rawData = stockRepository.findStocksForTenDays(startDate);

        for (Stock stock : rawData) {
            LocalDate date = stock.getDate();

            byDate.putIfAbsent(date, new HashMap<>());
            byDate.get(date).put(stock.getSymbol(), stock.getPrice());

        }

        return byDate;
    }

    public List<StockPriceGetDTO> getCurrentRoundStockPrices(Long gameId) {
        GameManager manager = InMemoryGameRegistry.getGame(gameId);

        if (manager == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found: " + gameId);
        }

        Map<String, Double> prices = manager.getCurrentStockPrices();

        if (prices == null || prices.isEmpty()) {
            return new ArrayList<>(); // or optionally: throw new ResponseStatusException(...)
        }

        List<StockPriceGetDTO> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            StockPriceGetDTO dto = new StockPriceGetDTO();
            dto.setSymbol(entry.getKey());
            dto.setPrice(entry.getValue());
            dto.setCategory(getCategoryMap().getOrDefault(entry.getKey(), "UNKNOWN"));
            result.add(dto);
        }

        return result;
    }

    // new

    // --- New Methods for Player Data ---

    /**
     * Gets the player's stock holdings.
     *
     * @param userId The ID of the user.
     * @return A list of StockHoldingDTO objects representing the player's stock
     *         holdings.
     */
    public List<StockHoldingDTO> getPlayerHoldings(Long userId, Long gameId) {
        // Retrieve the GameManager using the gameId
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found for gameId: " + gameId);
        }

        // Retrieve the PlayerState for this user
        PlayerState player = game.getPlayerState(userId);
        if (player == null) {
            throw new IllegalArgumentException("Player state not found for userId: " + userId);
        }

        // Retrieve current stock prices and convert to StockHoldingDTO
        Map<String, Double> prices = game.getCurrentStockPrices();
        return toStockHoldings(player, prices, STOCK_CATEGORIES);
    }

    /**
     * Converts player stock data into StockHoldingDTO objects.
     *
     * @param player     The PlayerState object.
     * @param prices     The map of stock prices.
     * @param categories The map of stock categories.
     * @return A list of StockHoldingDTO objects.
     */
    public List<StockHoldingDTO> toStockHoldings(PlayerState player,
            Map<String, Double> prices,
            Map<String, String> categories) {
        List<StockHoldingDTO> holdings = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : player.getPlayerStocks().entrySet()) {
            String symbol = entry.getKey();
            int quantity = entry.getValue();
            double price = prices.getOrDefault(symbol, 0.0);
            String category = categories.getOrDefault(symbol, "UNKNOWN");

            StockHoldingDTO dto = new StockHoldingDTO(symbol, quantity, category, price);
            holdings.add(dto);
        }

        return holdings;
    }

    /**
     * Gets the GameManager associated with a user.
     *
     * @param userId The user's ID.
     * @return The GameManager object.
     */
    public GameManager getGameManagerForUser(Long userId, Long gameId) {
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null || game.getPlayerState(userId) == null) {
            throw new IllegalArgumentException("Game not found for userId: " + userId + " and gameId: " + gameId);
        }
        return game;
    }

}
