package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.NewsDTO;
import ch.uzh.ifi.hase.soprafs24.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    
    private final NewsService newsService;
    
    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }
    
    /**
     * Endpoint to get news articles relevant to a specific game.
     * The NewsService will determine the game's date range and tickers
     * and fetch/filter news accordingly.
     *
     * @param gameId The ID of the game.
     * @return A list of NewsDTO objects.
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<List<NewsDTO>> getGameNews(@PathVariable Long gameId) {
        try {
            List<NewsDTO> newsDTOs = newsService.getNewsForGame(gameId);
            return ResponseEntity.ok(newsDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Manual trigger endpoint for fetching news from Alpha Vantage and saving it to the database.
     * Useful for development, testing, or populating the database initially.
     * This endpoint is optional and can be secured or removed for production.
     *
     * @param tickers List of stock tickers (e.g., "AAPL,MSFT").
     * @param startDate Start date for news search in YYYY-MM-DD format.
     * @param endDate End date for news search in YYYY-MM-DD format.
     * @return A confirmation message or an error.
     */
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchNewsManually(
            @RequestParam List<String> tickers,
            @RequestParam String startDate, // Expects YYYY-MM-DD
            @RequestParam String endDate) { // Expects YYYY-MM-DD
        try {
            // Parse the date strings to LocalDate objects
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            newsService.fetchAndSaveNewsForTickers(tickers, start, end);
            return ResponseEntity.ok("News fetching process initiated for tickers: " + String.join(",", tickers) +
                                     " between " + startDate + " and " + endDate);
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid date format. Please use YYYY-MM-DD. Error: " + e.getMessage());
        } catch (Exception e) {
            // Catching a broader exception for other potential issues during the call
            return ResponseEntity.internalServerError().body("Error initiating news fetch: " + e.getMessage());
        }
    }
}