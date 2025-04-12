// src/main/java/ch/uzh/ifi/hase/soprafs24/controller/NewsController.java
package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.FinvizNewsArticleDTO; // Import the NEW DTO
import ch.uzh.ifi.hase.soprafs24.service.NewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
// Remove unused imports like RequestParam, BingNewsResponseDTO, old NewsArticleDTO etc.
import java.util.List;

@RestController
public class NewsController {

    private final NewsService newsService; // Inject NewsService

    public NewsController(NewsService newsService) { // Constructor injection
        this.newsService = newsService;
    }

    // Endpoint to get Finviz news
    @GetMapping("/news")
    // Return type changed to List<FinvizNewsArticleDTO>
    // Removed @RequestParam String query
    public List<FinvizNewsArticleDTO> getNews() {
        // Call the updated service method for Finviz
        return newsService.fetchFinvizNews();
    }
}