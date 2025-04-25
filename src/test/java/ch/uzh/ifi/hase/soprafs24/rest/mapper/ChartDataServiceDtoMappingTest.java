package ch.uzh.ifi.hase.soprafs24.rest.mapper;
import ch.uzh.ifi.hase.soprafs24.service.ChartDataService;
import ch.uzh.ifi.hase.soprafs24.entity.StockDataPoint;
import ch.uzh.ifi.hase.soprafs24.repository.StockDataPointRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockDataPointDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChartDataServiceDtoMappingTest {

    private StockDataPointRepository repo;
    private ChartDataService service;

    @BeforeEach
    void setUp() {
        repo = mock(StockDataPointRepository.class);
        service = new ChartDataService(repo, "dummy-key");
    }

    @Test
    void testGetDailyChartData_MapsEntitiesToDtos() {
        // Arrange: two entities with different dates
        StockDataPoint e1 = new StockDataPoint();
        e1.setSymbol("TSLA");
        e1.setDate(LocalDate.of(2025, 4, 18));
        e1.setOpen(600.0);
        e1.setHigh(620.0);
        e1.setLow(590.0);
        e1.setClose(610.0);
        e1.setVolume(5_000L);

        StockDataPoint e2 = new StockDataPoint();
        e2.setSymbol("TSLA");
        e2.setDate(LocalDate.of(2025, 4, 19));
        e2.setOpen(610.0);
        e2.setHigh(630.0);
        e2.setLow(600.0);
        e2.setClose(620.0);
        e2.setVolume(6_000L);

        when(repo.findBySymbolOrderByDateAsc("TSLA")).thenReturn(List.of(e1, e2));

        // Act
        List<StockDataPointDTO> dtos = service.getDailyChartData("TSLA");

        // Assert: each field is carried over correctly
        assertEquals(2, dtos.size());

        StockDataPointDTO d1 = dtos.get(0);
        assertEquals("TSLA", d1.getSymbol());
        assertEquals(LocalDate.of(2025, 4, 18), d1.getDate());
        assertEquals(600.0, d1.getOpen());
        assertEquals(620.0, d1.getHigh());
        assertEquals(590.0, d1.getLow());
        assertEquals(610.0, d1.getClose());
        assertEquals(5000L,   d1.getVolume());

        StockDataPointDTO d2 = dtos.get(1);
        assertEquals(6_000L, d2.getVolume());

        verify(repo).findBySymbolOrderByDateAsc("TSLA");
    }
}
