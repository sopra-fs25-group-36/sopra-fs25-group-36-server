package ch.uzh.ifi.hase.soprafs24.controller;
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

import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.StockService;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

   

    @PostMapping("/fetch/popular-static")
    public ResponseEntity<String> fetchStaticPopularSymbols() {
        stockService.fetchKnownPopularStocks();
        return ResponseEntity.ok("Fetched and saved data for known popular stocks.");
    }


    @GetMapping("/categories")
    public ResponseEntity<Map<String, String>> getCategories() {
        // bring the stock servoce from StockService
        Map<String, String> categories = stockService.getCategoryMap();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/player-holdings/{userId}")
    public ResponseEntity<List<StockHoldingDTO>> getPlayerHoldings(
            @PathVariable Long userId,
            @RequestParam Long gameId) {
        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);
        return ResponseEntity.ok(holdings);
    }

    @GetMapping("/{gameId}/stocks")
    @ResponseStatus(HttpStatus.OK)
    public List<StockPriceGetDTO> getPrice(
    @PathVariable Long gameId,
    @RequestParam(required = false) String symbol,
    @RequestParam(required = false) Integer round) {
        if (symbol == null || round == null) {
            return stockService.getCurrentRoundStockPrices(gameId);
        }
        return stockService.getStockPrice(gameId, symbol, round);
    }
    @GetMapping("/all-data/{userId}")
    public ResponseEntity<Map<String, Object>> getAllStockData(
            @PathVariable Long userId,
            @RequestParam Long gameId) {
        Map<String, String> categories = stockService.getCategoryMap();
        List<StockHoldingDTO> holdings = stockService.getPlayerHoldings(userId, gameId);

        Map<String, Object> allData = Map.of(
                "categories", categories,
                "holdings", holdings
        );

        return ResponseEntity.ok(allData);
    }
    @GetMapping("/player-holdings/{userId}/round/{round}")
    public ResponseEntity<List<StockHoldingDTO>> getPlayerHoldingsByRound(
            @PathVariable Long userId,
            @PathVariable Integer round,
            @RequestParam Long gameId) {

        List<StockHoldingDTO> holdings = stockService.getPlayerHoldingsByRound(userId, gameId, round);
        return ResponseEntity.ok(holdings);
    }
    @GetMapping("/player-holdings/{userId}/all-rounds")
    public ResponseEntity<Map<Integer, List<StockHoldingDTO>>> getPlayerHoldingsAllRounds(
            @PathVariable Long userId,
            @RequestParam Long gameId) {

        Map<Integer, List<StockHoldingDTO>> allHoldings = stockService.getPlayerHoldingsAllRounds(userId, gameId);
        return ResponseEntity.ok(allHoldings);
    }

}