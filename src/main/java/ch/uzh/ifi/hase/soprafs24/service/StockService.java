package ch.uzh.ifi.hase.soprafs24.service;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;
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
import ch.uzh.ifi.hase.soprafs24.repository.StockRepository;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository, @Value("${ALPHAVANTAGE_API_KEY}") String API_KEY) {
        this.stockRepository = stockRepository;
        this.API_KEY = API_KEY;
    }

    // private static final String BASE_URL = "https://www.alphavantage.co/query";
    private String API_KEY;
    // // Uses environment variable
    // private final Map<String, List<Double>> stockMemory = new HashMap<>();
    // private int roundCounter = 0;

    //     /// julius
    // public Double fetchStockReturn(String stockSymbol) throws Exception {
    //     Map<LocalDate, Double> closingPrices = fetchTwoDaysClosingPricesFromAPI(stockSymbol);
    //     if (closingPrices.size() < 2) {
    //         throw new Exception("Not enough data to calculate returns.");
    //     }

    //     List<Double> prices = new ArrayList<>(closingPrices.values());
    //     double previousClose = prices.get(0);
    //     double latestClose = prices.get(1);

    //     // Store in memory
    //     stockMemory.put(stockSymbol, prices);
    //     return (latestClose - previousClose) / previousClose;

    // }
    /// julius

    // private Map<LocalDate, Double> fetchTwoDaysClosingPricesFromAPI(String stockSymbol) throws Exception {
    //     String urlString = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
    //             BASE_URL, stockSymbol, API_KEY);

    //     URL url = new URL(urlString);
    //     HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    //     connection.setRequestMethod("GET");

    //     if (connection.getResponseCode() != 200) {
    //         throw new Exception("Failed to fetch stock data.");
    //     }

    //     BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    //     StringBuilder response = new StringBuilder();
    //     String inputLine;
    //     while ((inputLine = in.readLine()) != null) {
    //         response.append(inputLine);
    //     }
    //     in.close();

    //     JSONObject jsonObject = new JSONObject(response.toString());
    //     JSONObject timeSeries = jsonObject.getJSONObject("Time Series (Daily)");

    //     List<String> dates = new ArrayList<>(timeSeries.keySet());
    //     Collections.sort(dates, Collections.reverseOrder());

    //     Map<LocalDate, Double> closingPrices = new LinkedHashMap<>();
    //     for (int i = 0; i < Math.min(2, dates.size()); i++) {
    //         String date = dates.get(i);
    //         double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
    //         closingPrices.put(LocalDate.parse(date, DateTimeFormatter.ISO_DATE), closePrice);
    //     }

    //     return closingPrices;
    // }

    /// julius
    // public String fetchStockData(String stockSymbol) throws IOException {
    //     String urlString = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
    //             BASE_URL, stockSymbol, API_KEY);

    //     URL url = new URL(urlString);
    //     HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    //     connection.setRequestMethod("GET");

    //     if (connection.getResponseCode() != 200) {
    //         throw new IOException("Failed to fetch stock data. HTTP Response Code: " + connection.getResponseCode());
    //     }

    //     BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    //     StringBuilder response = new StringBuilder();
    //     String inputLine;
    //     while ((inputLine = in.readLine()) != null) {
    //         response.append(inputLine);
    //     }
    //     in.close();

    //     return response.toString();
    // }

    // CREATING DATABASE TO STORE STOCKS
    // new function//Seung
    private static final List<String> POPULAR_SYMBOLS = List.of(
            "TSLA", "GOOG", "MSFT", "NVDA", "AMZN", "META", "NFLX", "INTC",
            "AMD", "AAPL");

    public void fetchKnownPopularStocks() {
        for (String symbol : POPULAR_SYMBOLS) {
            try {
//                fetchAndProcessStockData(symbol); //UNCOMMENT
            } catch (Exception e) {
                System.err.println("Failed to fetch data for " + symbol + ": " + e.getMessage());
            }
        }
    }
// ++++++++broken++++++++
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

    //////////////

    // public String saveStockData(String stockSymbol, String jsonData) throws IOException {
    //     LocalDate today = LocalDate.now();
    //     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    //     String filename = stockSymbol + "_daily_" + today.format(formatter) + ".json";

    //     JSONObject jsonObject = new JSONObject(jsonData);
    //     String prettyJson = jsonObject.toString(4);

    //     try (FileWriter file = new FileWriter(filename)) {
    //         file.write(prettyJson);
    //         file.flush();
    //     }

    //     return filename;
    // }

    public List<StockPriceGetDTO> getStockPrice(Long gameId, String symbol) {
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


}
