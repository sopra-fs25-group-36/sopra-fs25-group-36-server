package ch.uzh.ifi.hase.soprafs24.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map; // Added for Collections.emptyList() and Collections.emptyMap()

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.crazzyghost.alphavantage.AlphaVantage; // For DB operations
import com.crazzyghost.alphavantage.Config;
import com.crazzyghost.alphavantage.parameters.DataType;
import com.crazzyghost.alphavantage.parameters.OutputSize;
import com.crazzyghost.alphavantage.timeseries.response.StockUnit;
import com.crazzyghost.alphavantage.timeseries.response.TimeSeriesResponse;

import ch.uzh.ifi.hase.soprafs24.entity.Stock; // Explicit import
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.repository.StockRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private final StockRepository stockRepository;
    private final String API_KEY;

    public StockService(StockRepository stockRepository, @Value("${ALPHAVANTAGE_API_KEY}") String API_KEY) {
        this.stockRepository = stockRepository;
        this.API_KEY = API_KEY;
    }


    // for updating more data from dbs_May.11  --total 29 stocks
    private static final List<String> POPULAR_SYMBOLS = List.of(
            "TSLA", "GOOG", "MSFT", "NVDA", "AMZN", "META", "NFLX", "INTC", "AMD", "AAPL",
            "JPM", "GS",
            "PFE", "JNJ",
            "XOM", "CVX",
            "PG",
            "WDAY", "KO", "BTI", "MCD", "SHEL", "WMT", "COST", "BABA", "LLY", "ABBV", "V", "MA"
    );

    // Consistent category mapping
    private static final Map<String, String> STOCK_CATEGORIES = Collections.unmodifiableMap(new HashMap<>() {{
        put("TSLA", "TECH");
        put("GOOG", "TECH");
        put("MSFT", "TECH");
        put("NVDA", "TECH");
        put("AMZN", "TECH"); /*put("META", "TECH");*/
        put("NFLX", "TECH");
        put("INTC", "TECH");
        put("AMD", "TECH");
        put("AAPL", "TECH");
        put("WDAY", "TECH");
        put("XOM", "ENERGY");
        put("CVX", "ENERGY");
        put("SHEL", "ENERGY");
        put("JPM", "FINANCE");
        put("GS", "FINANCE");
        put("V", "FINANCE");
        put("MA", "FINANCE");
        put("PFE", "HEALTHCARE");
        put("JNJ", "HEALTHCARE");
        put("LLY", "HEALTHCARE");
        put("ABBV", "HEALTHCARE");
        put("PG", "CONSUMER");
        put("KO", "CONSUMER");
        put("BTI", "CONSUMER");
        put("MCD", "CONSUMER");
        put("WMT", "RETAIL");
        put("COST", "RETAIL");
        put("BABA", "RETAIL");


        // Add any other symbols used in your game if they have specific categories
        //  put("IBM", "MISC"); // Example if IBM is used
    }});

    public Map<String, String> getCategoryMap() {
        return STOCK_CATEGORIES;
    }

    @Scheduled(cron = "${stock.update.cron:0 0 1 * * ?}") // Use property or default to 1 AM
    public void scheduleStockUpdate() {
        log.info("Starting scheduled stock data fetch based on cron expression!");
        fetchKnownPopularStocks(); // This method itself should decide if it fetches or not
        log.info("Finished scheduled stock data fetch!");
    }

    public void fetchKnownPopularStocks() {
        log.info("Attempting to fetch data for {} popular stocks.", POPULAR_SYMBOLS.size());
        boolean performFetch = true; // Set to false to disable actual fetching during development if needed
        if (!performFetch) {
            log.warn("Actual data fetching is currently DISABLED in fetchKnownPopularStocks method.");
            return;
        }

        for (String symbol : POPULAR_SYMBOLS) {
            try {
                fetchAndProcessStockData(symbol);
            }
            catch (Exception e) {
                log.error("Failed to fetch data for symbol {}: {}", symbol, e.getMessage(), e);
            }
        }
    }

    @Transactional // Ensures atomicity for DB operations related to one symbol's fetch
    public void fetchAndProcessStockData(String symbol) {
        log.info("Fetching and processing stock data for symbol: {}", symbol);
        Config cfg = Config.builder()
                .key(API_KEY)
                .timeOut(30) // Increased timeout for AlphaVantage
                .build();
        AlphaVantage.api().init(cfg);

        try {
            TimeSeriesResponse response = AlphaVantage.api()
                    .timeSeries()
                    .daily()
                    .adjusted()
                    .forSymbol(symbol)
                    .outputSize(OutputSize.FULL) // Full history
                    .dataType(DataType.JSON)
                    .fetchSync();

            if (response.getErrorMessage() != null) {
                log.error("AlphaVantage API error for symbol {}: {}", symbol, response.getErrorMessage());
                return;
            }

            List<StockUnit> stockUnits = response.getStockUnits();
            if (stockUnits == null || stockUnits.isEmpty()) {
                log.warn("No stock units returned from AlphaVantage for symbol: {}", symbol);
                return;
            }

            int newRecordsSaved = 0;
            for (StockUnit unit : stockUnits) {
                try {
                    LocalDate date = LocalDate.parse(unit.getDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                    // Check if data for this symbol and date already exists to avoid duplicates
                    if (stockRepository.findBySymbolAndDate(symbol, date).isEmpty()) {
                        Stock stock = new Stock();
                        stock.setSymbol(symbol);
                        stock.setDate(date);
                        stock.setPrice(unit.getClose()); // Using closing price
                        stock.setVolume(unit.getVolume());
                        stock.setCurrency("USD"); // Assuming USD
                        // stock.setCategory(STOCK_CATEGORIES.getOrDefault(symbol, "OTHER")); // If your Stock entity has a category field
                        stockRepository.save(stock);
                        newRecordsSaved++;
                    }
                }
                catch (Exception e) {
                    log.error("Error processing or saving stock unit for symbol {} on date {}: {}", symbol, unit.getDate(), e.getMessage(), e);
                }
            }
            if (newRecordsSaved > 0) {
                log.info("Saved {} new daily records for symbol {}.", newRecordsSaved, symbol);
            }
            else {
                log.info("No new daily records to save for symbol {} (data likely up-to-date).", symbol);
            }

        }
        catch (Exception e) { // Catch broader exceptions from AlphaVantage client or processing
            log.error("Exception during AlphaVantage fetch or processing for symbol {}: {}", symbol, e.getMessage(), e);
        }
    }

    // This method is for getting historical prices, potentially for game setup or specific queries.
    // It is NOT what /api/stocks/{gameId}/stocks calls for current round prices.
    public List<StockPriceGetDTO> getStockPrice(Long gameId, String symbol, Integer round) {
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null) {
            log.warn("Game with ID {} not found for getStockPrice(symbol, round).", gameId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with ID " + gameId + " not found.");
        }

        LocalDate dateForRound = game.getDateForRound(round);
        if (dateForRound == null) {
            log.warn("Could not determine date for round {} in game {}. Returning empty list.", round, gameId);
            return Collections.emptyList();
        }

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = game.getStockTimeline();
        List<StockPriceGetDTO> result = new ArrayList<>();

        for (Map.Entry<LocalDate, Map<String, Double>> entry : timeline.entrySet()) {
            LocalDate entryDate = entry.getKey();
            if (entryDate.isAfter(dateForRound)) { // Only data up to and including the round's date
                continue; // Or break if timeline is sorted and we've passed the target date
            }
            Map<String, Double> pricesOnDate = entry.getValue();
            if (pricesOnDate.containsKey(symbol)) {
                StockPriceGetDTO dto = new StockPriceGetDTO();
                dto.setSymbol(symbol);
                dto.setDate(entryDate);
                dto.setPrice(pricesOnDate.get(symbol));
                dto.setCategory(STOCK_CATEGORIES.getOrDefault(symbol, "OTHER")); // Use consistent category mapping
                dto.setRound(round); // Or determine round from date if mapping exists
                result.add(dto);
            }
        }
        return result;
    }

    public LinkedHashMap<LocalDate, Map<String, Double>> getStockTimelineFromDatabase() {
        LinkedHashMap<LocalDate, Map<String, Double>> byDate = new LinkedHashMap<>();
        // Ensure this logic is robust for game initialization.
        // "findRandomStartDateWith10Days" might not be suitable for all game setups.
        LocalDate startDate = stockRepository.findRandomStartDateWith10Days();
        if (startDate == null) {
            log.error("Critical: Could not find a random start date with sufficient data. Stock timeline will be empty for new game.");
            // This could lead to games starting without data. Consider throwing an exception or having a fallback.
            return byDate;
        }

        List<Stock> rawData = stockRepository.findRandom10SymbolsWithFirst10DatesFrom(startDate);
        if (rawData.isEmpty()) {
            log.warn("No stock data found from startDate {} for game timeline. Timeline will be empty.", startDate);
            return byDate;
        }

        for (Stock stock : rawData) {
            LocalDate date = stock.getDate();
            byDate.computeIfAbsent(date, k -> new HashMap<>()).put(stock.getSymbol(), stock.getPrice());
        }
        log.info("Generated stock timeline for a new game with {} dates, starting from {}. Total data points: {}",
                byDate.size(), startDate, rawData.size());
        return byDate;
    }

    public List<StockPriceGetDTO> getCurrentRoundStockPrices(Long gameId) {
        GameManager manager = InMemoryGameRegistry.getGame(gameId);
        if (manager == null) {
            log.warn("Game with ID {} not found for getCurrentRoundStockPrices.", gameId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found: " + gameId);
        }

        LocalDate currentMarketDate = manager.getCurrentMarketDate(); // THIS IS THE KEY
        if (currentMarketDate == null) {
            log.error("CRITICAL: GameManager for gameId {} did not provide a currentMarketDate. " +
                    "Stock prices will be returned without a date, and frontend chart filtering will NOT work as intended.", gameId);
            // This indicates a fundamental issue in game state or logic if the current date is unknown.
            // Depending on game rules, you might throw an exception or return data without dates (current behavior).
        }

        Map<String, Double> currentPrices = manager.getCurrentStockPrices();
        if (currentPrices == null || currentPrices.isEmpty()) {
            log.warn("No current stock prices available in GameManager for gameId {} and round {}.", gameId, manager.getCurrentRound());
            return Collections.emptyList();
        }

        List<StockPriceGetDTO> result = new ArrayList<>();
        int currentRound = manager.getCurrentRound(); // Get current round once

        for (Map.Entry<String, Double> entry : currentPrices.entrySet()) {
            StockPriceGetDTO dto = new StockPriceGetDTO();
            String symbol = entry.getKey();
            dto.setSymbol(symbol);
            dto.setPrice(entry.getValue());
            dto.setCategory(STOCK_CATEGORIES.getOrDefault(symbol, "OTHER")); // Use consistent category mapping
            dto.setRound(currentRound);
            dto.setDate(currentMarketDate); // <<< SET THE DATE FOR THE CURRENT ROUND'S PRICES
            result.add(dto);
        }
        log.debug("Returning {} stock prices for gameId {} for date {} and round {}.",
                result.size(), gameId, currentMarketDate, currentRound);
        return result;
    }

    public List<StockHoldingDTO> getPlayerHoldings(Long userId, Long gameId) {
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null) {
            log.warn("Game not found for gameId: {} when getting player holdings for userId: {}.", gameId, userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found for gameId: " + gameId);
        }

        PlayerState player = game.getPlayerState(userId);
        if (player == null) {
            log.warn("Player state not found for userId: {} in gameId: {}.", userId, gameId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player state not found for userId: " + userId);
        }

        Map<String, Double> prices = game.getCurrentStockPrices();
        if (prices == null) {
            log.warn("Current stock prices are null in gameId {} for player holdings. Using empty prices map.", gameId);
            prices = Collections.emptyMap();
        }
        return toStockHoldings(player, prices);
    }

    private List<StockHoldingDTO> toStockHoldings(PlayerState player, Map<String, Double> prices) {
        List<StockHoldingDTO> holdings = new ArrayList<>();
        Map<String, Integer> playerStocks = player.getPlayerStocks();

        if (playerStocks == null || playerStocks.isEmpty()) {
            return holdings;
        }

        for (Map.Entry<String, Integer> entry : playerStocks.entrySet()) {
            String symbol = entry.getKey();
            int quantity = entry.getValue();
            if (quantity <= 0) continue; // Don't show holdings with zero or negative quantity

            double price = prices.getOrDefault(symbol, 0.0);
            String category = STOCK_CATEGORIES.getOrDefault(symbol, "OTHER");

            StockHoldingDTO dto = new StockHoldingDTO(symbol, quantity, category, price);
            holdings.add(dto);
        }
        return holdings;
    }

    // This method seems primarily for validation or direct GameManager access.
    public GameManager getGameManagerForUser(Long userId, Long gameId) {
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null) {
            log.warn("Game not found for gameId {} during GameManager lookup for user {}.", gameId, userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found for gameId: " + gameId);
        }
        if (game.getPlayerState(userId) == null) {
            log.warn("Player {} not found in game {} during GameManager lookup.", userId, gameId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player " + userId + " not found in game " + gameId);
        }
        return game;
    }

    public List<StockHoldingDTO> getPlayerHoldingsByRound(Long userId, Long gameId, int round) {
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found for gameId: " + gameId);
        }

        PlayerState player = game.getPlayerState(userId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found in game.");
        }

        Map<String, Integer> snapshot = player.getHoldingsForRound(round);
        LocalDate dateForRound = game.getDateForRound(round);
        Map<String, Double> prices = game.getStockTimeline().getOrDefault(dateForRound, Collections.emptyMap());

        List<StockHoldingDTO> holdings = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            String symbol = entry.getKey();
            int quantity = entry.getValue();
            if (quantity <= 0) continue;

            double price = prices.getOrDefault(symbol, 0.0);
            String category = STOCK_CATEGORIES.getOrDefault(symbol, "OTHER");

            holdings.add(new StockHoldingDTO(symbol, quantity, category, price));
        }

        return holdings;
    }

    public Map<Integer, List<StockHoldingDTO>> getPlayerHoldingsAllRounds(
            Long userId,
            Long gameId
    ) {
        // 0) Lookup game & player
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Game not found: " + gameId);
        }
        PlayerState player = game.getPlayerState(userId);
        if (player == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Player not found: " + userId);
        }

        // We'll preserve insertion order of rounds 1…(currentRound+1)
        Map<Integer, List<StockHoldingDTO>> roundHoldings = new LinkedHashMap<>();

        // 1) Display round 1 should always be empty (no prior round)
        roundHoldings.put(1, Collections.emptyList());

        // 2) For each actual round 1…currentRound:
        for (int round = 1; round <= game.getCurrentRound(); round++) {
            // 2a) Snapshot of holdings immediately after this round
            Map<String, Integer> snapshot = player.getHoldingsForRound(round);

            // 2b) We want to display these under “round+1”, using the next day’s prices:
            int displayRound = round + 1;

            // 2c) Fetch the date for the *display* round
            LocalDate priceDate = game.getDateForRound(displayRound);

            // 2d) Lookup prices on that date in your timeline
            Map<String, Double> prices = game.getStockTimeline()
                    .getOrDefault(priceDate, Collections.emptyMap());

            // 2e) Build DTOs for any positive holdings
            List<StockHoldingDTO> holdings = new ArrayList<>();
            for (Map.Entry<String, Integer> e : snapshot.entrySet()) {
                String symbol = e.getKey();
                int quantity = e.getValue();
                if (quantity <= 0) continue;  // skip zero or negative

                // 2f) Use the price from the *next* day
                double price = prices.getOrDefault(symbol, 0.0);
                String category = STOCK_CATEGORIES.getOrDefault(symbol, "OTHER");

                holdings.add(new StockHoldingDTO(symbol, quantity, category, price));
            }

            // 2g) Store under the shifted display round
            roundHoldings.put(displayRound, holdings);
        }

        return roundHoldings;
    }

}
