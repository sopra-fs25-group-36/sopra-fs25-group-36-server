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

    @GetMapping("/{gameId}")
    public ResponseEntity<List<NewsDTO>> getGameNews(@PathVariable Long gameId) {
        try {
            List<NewsDTO> newsDTOs = newsService.getNewsForGame(gameId);
            return ResponseEntity.ok(newsDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/fetch")
    public ResponseEntity<String> fetchNewsManually(
            @RequestParam List<String> tickers,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            newsService.fetchAndSaveNewsForTickers(tickers, start, end);
            return ResponseEntity.ok("News fetching process initiated for tickers: " + String.join(",", tickers) +
                    " between " + startDate + " and " + endDate);
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid date format. Please use YYYY-MM-DD. Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error initiating news fetch: " + e.getMessage());
        }
    }
}