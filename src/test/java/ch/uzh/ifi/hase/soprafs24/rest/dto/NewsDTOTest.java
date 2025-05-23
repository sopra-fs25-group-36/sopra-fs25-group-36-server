package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsDTOTest {

    @Test
    public void testGettersAndSetters() {
        NewsDTO newsDTO = new NewsDTO();
        Long id = 1L;
        newsDTO.setId(id);
        assertEquals(id, newsDTO.getId());
        String title = "Test Title";
        newsDTO.setTitle(title);
        assertEquals(title, newsDTO.getTitle());
        String url = "http://example.com/news";
        newsDTO.setUrl(url);
        assertEquals(url, newsDTO.getUrl());
        String summary = "This is a test summary.";
        newsDTO.setSummary(summary);
        assertEquals(summary, newsDTO.getSummary());
        String bannerImage = "http://example.com/image.jpg";
        newsDTO.setBannerImage(bannerImage);
        assertEquals(bannerImage, newsDTO.getBannerImage());
        String source = "Test Source";
        newsDTO.setSource(source);
        assertEquals(source, newsDTO.getSource());
        String sourceDomain = "example.com";
        newsDTO.setSourceDomain(sourceDomain);
        assertEquals(sourceDomain, newsDTO.getSourceDomain());
        LocalDateTime publishedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        newsDTO.setPublishedTime(publishedTime);
        assertEquals(publishedTime, newsDTO.getPublishedTime());
        Double overallSentimentScore = 0.75;
        newsDTO.setOverallSentimentScore(overallSentimentScore);
        assertEquals(overallSentimentScore, newsDTO.getOverallSentimentScore());
        String overallSentimentLabel = "Positive";
        newsDTO.setOverallSentimentLabel(overallSentimentLabel);
        assertEquals(overallSentimentLabel, newsDTO.getOverallSentimentLabel());
        List<Map<String, Object>> tickerSentiments = new ArrayList<>();
        Map<String, Object> sentiment1 = new HashMap<>();
        sentiment1.put("ticker", "AAPL");
        sentiment1.put("score", 0.8);
        tickerSentiments.add(sentiment1);
        newsDTO.setTickerSentiments(tickerSentiments);
        assertEquals(tickerSentiments, newsDTO.getTickerSentiments());
        assertNotNull(newsDTO.getTickerSentiments());
        if (newsDTO.getTickerSentiments() != null && !newsDTO.getTickerSentiments().isEmpty()) {
            assertEquals(1, newsDTO.getTickerSentiments().size());
            assertEquals("AAPL", newsDTO.getTickerSentiments().get(0).get("ticker"));
        } else {
            fail("Ticker sentiments list is null or empty after setting");
        }
    }
}