package ch.uzh.ifi.hase.soprafs24.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.service.StockService;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<String> getStockData(@PathVariable String symbol) {
        try {
            String stockData = stockService.fetchStockData(symbol);
            return ResponseEntity.ok(stockData);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error fetching stock data: " + e.getMessage());
        }
    }

    @PostMapping("/{symbol}/save")
    public ResponseEntity<String> saveStockData(@PathVariable String symbol) {
        try {
            String stockData = stockService.fetchStockData(symbol);
            String filename = stockService.saveStockData(symbol, stockData);
            return ResponseEntity.ok("Stock data saved to file: " + filename);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error saving stock data: " + e.getMessage());
        }
    }
}
