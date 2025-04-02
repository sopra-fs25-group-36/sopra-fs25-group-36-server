package ch.uzh.ifi.hase.soprafs24.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Test
    public void testGetStockTimelineFromDatabase() {
        List<Map<String, Double>> timeline = stockService.getStockTimelineFromDatabase();
        System.out.println("Username: " + System.getenv("myusername")); // or use System.getProperty

        assertNotNull(timeline);
        assertEquals(10, timeline.size(), "Should return 10 days of stock data");

        for (Map<String, Double> day : timeline) {
            System.out.println("Day Snapshot:");
            day.forEach((symbol, price) -> System.out.println(symbol + " → " + price));
        }
    }
}
