package ch.uzh.ifi.hase.soprafs24.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.LinkedHashMap;
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

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = stockService.getStockTimelineFromDatabase();
        System.out.println("Username: " + System.getenv("myusername")); // or use System.getProperty

        assertNotNull(timeline);
        assertEquals(10, timeline.size(), "Should return 10 days of stock data");


        for (Map.Entry<LocalDate, Map<String, Double>> entry : timeline.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Double> prices = entry.getValue();

            System.out.println("Day: " + date);
            prices.forEach((symbol, price) -> System.out.println("  " + symbol + " → " + price));
        }

    }
    @Test
    public void testGetStockTimelineFromDatabase_consistentStockCount() {

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = stockService.getStockTimelineFromDatabase();

        assertNotNull(timeline, "Timeline should not be null");
        assertEquals(10, timeline.size(), "Should return 10 days of stock data");

        // Track expected number of stocks on first day
        int expectedStockCount = -1;
        int dayNumber = 1;

        for (Map.Entry<LocalDate, Map<String, Double>> entry : timeline.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Double> prices = entry.getValue();

            System.out.println("Day " + dayNumber++ + ": " + date);
            prices.forEach((symbol, price) -> System.out.println("  " + symbol + " → " + price));

            if (expectedStockCount == -1) {
                expectedStockCount = prices.size();  // initialize on first day
            } else {
                assertEquals(expectedStockCount, prices.size(), "Inconsistent number of stocks on " + date);
            }
        }

        System.out.println("All days contain " + expectedStockCount + " stocks consistently.");
    }

}
