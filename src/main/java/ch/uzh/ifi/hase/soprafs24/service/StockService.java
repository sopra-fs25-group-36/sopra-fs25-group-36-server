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

import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final String API_KEY = System.getenv("API_KEY"); // Uses environment variable
    private final Map<String, List<Double>> stockMemory = new HashMap<>();
    private int roundCounter = 0;

    public Double fetchStockReturn(String stockSymbol) throws Exception {
        Map<String, Double> closingPrices = fetchTwoDaysClosingPrices(stockSymbol);
        if (closingPrices.size() < 2) {
            throw new Exception("Not enough data to calculate returns.");
        }

        List<Double> prices = new ArrayList<>(closingPrices.values());
        double previousClose = prices.get(0);
        double latestClose = prices.get(1);

        double stockReturn = (latestClose - previousClose) / previousClose;

        // Store in memory
        stockMemory.put(stockSymbol, prices);
        roundCounter++;

        if (roundCounter >= 10) {
            stockMemory.clear();
            roundCounter = 0;
            System.out.println("Memory cleared after 10 rounds.");
        }

        return stockReturn;
    }

    private Map<String, Double> fetchTwoDaysClosingPrices(String stockSymbol) throws Exception {
        String urlString = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
                BASE_URL, stockSymbol, API_KEY);

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != 200) {
            throw new Exception("Failed to fetch stock data.");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonObject = new JSONObject(response.toString());
        JSONObject timeSeries = jsonObject.getJSONObject("Time Series (Daily)");

        List<String> dates = new ArrayList<>(timeSeries.keySet());
        Collections.sort(dates, Collections.reverseOrder());

        Map<String, Double> closingPrices = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(2, dates.size()); i++) {
            String date = dates.get(i);
            double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
            closingPrices.put(date, closePrice);
        }

        return closingPrices;
    }

    public String fetchStockData(String stockSymbol) throws IOException {
        String urlString = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
                BASE_URL, stockSymbol, API_KEY);

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to fetch stock data. HTTP Response Code: " + connection.getResponseCode());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public String saveStockData(String stockSymbol, String jsonData) throws IOException {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String filename = stockSymbol + "_daily_" + today.format(formatter) + ".json";

        JSONObject jsonObject = new JSONObject(jsonData);
        String prettyJson = jsonObject.toString(4);

        try (FileWriter file = new FileWriter(filename)) {
            file.write(prettyJson);
            file.flush();
        }

        return filename;
    }
}
