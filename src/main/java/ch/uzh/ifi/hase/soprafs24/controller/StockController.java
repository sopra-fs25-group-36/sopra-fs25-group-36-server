package ch.uzh.ifi.hase.soprafs24.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.StockService;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // @GetMapping("/{symbol}/return")
    // public ResponseEntity<String> getStockReturn(@PathVariable String symbol) {
    // try {
    // Double stockReturn = stockService.fetchStockReturn(symbol);
    // return ResponseEntity.ok("Stock return: " + (stockReturn * 100) + "%");
    // } catch (Exception e) {
    // return ResponseEntity.status(500).body("Error: " + e.getMessage());
    // }
    // }

    // @GetMapping("/{symbol}")
    // public ResponseEntity<String> getStockData(@PathVariable String symbol) {
    // try {
    // String stockData = stockService.fetchStockData(symbol);
    // return ResponseEntity.ok(stockData);
    // } catch (IOException e) {
    // return ResponseEntity.status(500).body("Error fetching stock data: " +
    // e.getMessage());
    // }
    // }

    @PostMapping("/fetch/popular-static")
    public ResponseEntity<String> fetchStaticPopularSymbols() {
        stockService.fetchKnownPopularStocks();
        return ResponseEntity.ok("Fetched and saved data for known popular stocks.");
    }

    // @PostMapping("/{symbol}/save")
    // public ResponseEntity<String> saveStockData(@PathVariable String symbol) {
    // try {
    // String stockData = stockService.fetchStockData(symbol);
    // String filename = stockService.saveStockData(symbol, stockData);
    // return ResponseEntity.ok("Stock data saved to file: " + filename);
    // } catch (IOException e) {
    // return ResponseEntity.status(500).body("Error saving stock data: " +
    // e.getMessage());
    // }
    // }

    // Get price of a specific stock at a specific round
    @GetMapping("/{gameID}/stocks")
    @ResponseStatus(HttpStatus.OK)
    public List<StockPriceGetDTO> getStockPrice(
            @PathVariable Long gameID) {
        Map<String, Double> stockPrices = stockService.getStockPrice(gameID);

        List<StockPriceGetDTO> result = new ArrayList<StockPriceGetDTO>();

        // return stockPrices.values().
        // .map(priceMap -> {
        // StockPriceGetDTO dto = new StockPriceGetDTO();
        // // dto.setSymbol(priceMap.get("symbol"));
        // dto.setPrice(priceMap.get("price"));
        // return dto;
        // })
        // .toList();
        for (Map.Entry<String, Double> entry : stockPrices.entrySet()) {
            StockPriceGetDTO dto = new StockPriceGetDTO();
            dto.setSymbol(entry.getKey());
            dto.setPrice(entry.getValue());
            result.add(dto);
        }

        return result;
    }

}
