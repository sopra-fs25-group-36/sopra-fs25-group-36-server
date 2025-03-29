package ch.uzh.ifi.hase.soprafs24.service;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;
import org.springframework.stereotype.Service;


@Service
public class StockService {

    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final String API_KEY = System.getenv("ALPHAVANTAGE_API_KEY");


    public String fetchStockData(String stockSymbol) throws IOException {
        String urlString = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
                BASE_URL, stockSymbol, API_KEY);

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to fetch stock data. HTTP Response Code: " + responseCode);
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
