package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.StockDataPoint;
import ch.uzh.ifi.hase.soprafs24.repository.StockDataPointRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockDataPointDTO;

import com.crazzyghost.alphavantage.AlphaVantage;
import com.crazzyghost.alphavantage.Config;

import com.crazzyghost.alphavantage.timeseries.response.StockUnit;
import com.crazzyghost.alphavantage.timeseries.response.TimeSeriesResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChartDataServiceTest {

    @Mock
    private StockDataPointRepository stockDataPointRepository;

    @InjectMocks
    private ChartDataService chartDataService;

    private final String testApiKey = "TEST_API_KEY";

    @BeforeEach
    void setUp() {
        chartDataService = new ChartDataService(stockDataPointRepository, testApiKey);
        Config cfg = Config.builder().key(testApiKey).timeOut(5).build();
        AlphaVantage.api().init(cfg);
    }

    @Test
    public void saveStockUnitsForSymbol_noExistingData_savesAllNewData() {
        String symbol = "AAPL";
        LocalDate date1 = LocalDate.of(2023, 1, 1);
        LocalDate date2 = LocalDate.of(2023, 1, 2);

        StockUnit unit1 = mock(StockUnit.class);
        when(unit1.getDate()).thenReturn(date1.toString());
        when(unit1.getOpen()).thenReturn(150.0);
        when(unit1.getHigh()).thenReturn(152.0);
        when(unit1.getLow()).thenReturn(149.0);
        when(unit1.getClose()).thenReturn(151.0);
        when(unit1.getVolume()).thenReturn(1000000L);

        StockUnit unit2 = mock(StockUnit.class);
        when(unit2.getDate()).thenReturn(date2.toString());
        when(unit2.getOpen()).thenReturn(151.0);
        when(unit2.getHigh()).thenReturn(153.0);
        when(unit2.getLow()).thenReturn(150.0);
        when(unit2.getClose()).thenReturn(152.0);
        when(unit2.getVolume()).thenReturn(1200000L);

        List<StockUnit> stockUnits = List.of(unit1, unit2);

        when(stockDataPointRepository.existsBySymbolAndDate(eq(symbol), eq(date1))).thenReturn(false);
        when(stockDataPointRepository.existsBySymbolAndDate(eq(symbol), eq(date2))).thenReturn(false);

        chartDataService.saveStockUnitsForSymbol(symbol, stockUnits);

        verify(stockDataPointRepository, times(1)).saveAll(anyList());
        ArgumentCaptor<List<StockDataPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockDataPointRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(symbol, captor.getValue().get(0).getSymbol());
        assertEquals(date1, captor.getValue().get(0).getDate());
        assertEquals(151.0, captor.getValue().get(0).getClose());
    }

    @Test
    public void saveStockUnitsForSymbol_someExistingData_savesOnlyNewData() {
        String symbol = "MSFT";
        LocalDate date1 = LocalDate.of(2023, 1, 1); // Existing
        LocalDate date2 = LocalDate.of(2023, 1, 2); // New

        StockUnit unit1 = mock(StockUnit.class);
        when(unit1.getDate()).thenReturn(date1.toString());

        StockUnit unit2 = mock(StockUnit.class);
        when(unit2.getDate()).thenReturn(date2.toString());
        when(unit2.getOpen()).thenReturn(300.0);
        when(unit2.getHigh()).thenReturn(302.0);
        when(unit2.getLow()).thenReturn(299.0);
        when(unit2.getClose()).thenReturn(301.0);
        when(unit2.getVolume()).thenReturn(900000L);

        List<StockUnit> stockUnits = List.of(unit1, unit2);

        when(stockDataPointRepository.existsBySymbolAndDate(eq(symbol), eq(date1))).thenReturn(true);
        when(stockDataPointRepository.existsBySymbolAndDate(eq(symbol), eq(date2))).thenReturn(false);

        chartDataService.saveStockUnitsForSymbol(symbol, stockUnits);

        verify(stockDataPointRepository, times(1)).saveAll(anyList());
        ArgumentCaptor<List<StockDataPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockDataPointRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(symbol, captor.getValue().get(0).getSymbol());
        assertEquals(date2, captor.getValue().get(0).getDate());
        assertEquals(301.0, captor.getValue().get(0).getClose());
    }

    @Test
    public void saveStockUnitsForSymbol_allExistingData_savesNothing() {
        String symbol = "GOOG";
        LocalDate date1 = LocalDate.of(2023, 1, 1);

        StockUnit unit1 = mock(StockUnit.class);
        when(unit1.getDate()).thenReturn(date1.toString());
        List<StockUnit> stockUnits = List.of(unit1);

        when(stockDataPointRepository.existsBySymbolAndDate(eq(symbol), eq(date1))).thenReturn(true);

        chartDataService.saveStockUnitsForSymbol(symbol, stockUnits);

        verify(stockDataPointRepository, never()).saveAll(anyList());
    }

    @Test
    public void saveStockUnitsForSymbol_emptyList_savesNothing() {
        String symbol = "AMZN";
        List<StockUnit> stockUnits = Collections.emptyList();

        chartDataService.saveStockUnitsForSymbol(symbol, stockUnits);

        verify(stockDataPointRepository, never()).existsBySymbolAndDate(anyString(), any(LocalDate.class));
        verify(stockDataPointRepository, never()).saveAll(anyList());
    }


    @Test
    public void getDailyChartData_dataExists_returnsDtoList() {
        String symbol = "TSLA";
        LocalDate date1 = LocalDate.of(2023, 1, 1);
        LocalDate date2 = LocalDate.of(2023, 1, 2);

        StockDataPoint dp1 = new StockDataPoint();
        dp1.setSymbol(symbol);
        dp1.setDate(date1);
        dp1.setClose(200.0);

        StockDataPoint dp2 = new StockDataPoint();
        dp2.setSymbol(symbol);
        dp2.setDate(date2);
        dp2.setClose(205.0);

        List<StockDataPoint> dataPoints = List.of(dp1, dp2);
        when(stockDataPointRepository.findBySymbolOrderByDateAsc(symbol)).thenReturn(dataPoints);

        List<StockDataPointDTO> dtos = chartDataService.getDailyChartData(symbol);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals(symbol, dtos.get(0).getSymbol());
        assertEquals(date1, dtos.get(0).getDate());
        assertEquals(200.0, dtos.get(0).getClose());
        assertEquals(date2, dtos.get(1).getDate());
        assertEquals(205.0, dtos.get(1).getClose());
    }

    @Test
    public void getDailyChartData_noDataExists_returnsEmptyList() {
        String symbol = "NFLX";
        when(stockDataPointRepository.findBySymbolOrderByDateAsc(symbol)).thenReturn(Collections.emptyList());

        List<StockDataPointDTO> dtos = chartDataService.getDailyChartData(symbol);

        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    
}