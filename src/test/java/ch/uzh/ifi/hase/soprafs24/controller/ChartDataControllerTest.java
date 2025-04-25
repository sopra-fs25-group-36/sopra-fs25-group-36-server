package ch.uzh.ifi.hase.soprafs24.controller;


import ch.uzh.ifi.hase.soprafs24.rest.dto.StockDataPointDTO;
import ch.uzh.ifi.hase.soprafs24.service.ChartDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChartDataController.class)
class ChartDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChartDataService chartDataService;

    @Test
    void testTriggerDailyDataFetch_Success() throws Exception {
        // Arrange: service does not throw
        doNothing().when(chartDataService).fetchAndStoreDailyDataForAllSymbols();

        // Act & Assert
        mockMvc.perform(post("/api/charts/fetch-daily"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Daily chart data fetching process started successfully."));

        // verify controller called service exactly once
        verify(chartDataService, times(1)).fetchAndStoreDailyDataForAllSymbols();
    }

    @Test
    void testTriggerDailyDataFetch_Failure() throws Exception {
        // Arrange: service throws
        doThrow(new RuntimeException("API down")).when(chartDataService).fetchAndStoreDailyDataForAllSymbols();

        // Act & Assert
        mockMvc.perform(post("/api/charts/fetch-daily"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to start data fetching: API down")));

        verify(chartDataService, times(1)).fetchAndStoreDailyDataForAllSymbols();
    }

    @Test
    void testGetDailyChartData_ReturnsListAndUppercasesSymbol() throws Exception {
        // Arrange: prepare two DTOs
        StockDataPointDTO dto1 = new StockDataPointDTO(
                "AAPL", LocalDate.of(2025, 4, 20), 100.0, 110.0, 90.0, 105.0, 1_000L);
        StockDataPointDTO dto2 = new StockDataPointDTO(
                "AAPL", LocalDate.of(2025, 4, 21), 106.0, 115.0, 95.0, 110.0, 2_000L);
        List<StockDataPointDTO> dtos = List.of(dto1, dto2);

        when(chartDataService.getDailyChartData("AAPL")).thenReturn(dtos);

        // Act & Assert: lowercase path variable should be uppercased internally
        mockMvc.perform(get("/api/charts/aapl/daily")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].date").value("2025-04-20"))
                .andExpect(jsonPath("$[0].open").value(100.0))
                .andExpect(jsonPath("$[1].volume").value(2000));

        verify(chartDataService).getDailyChartData("AAPL");
    }

    @Test
    void testGetDailyChartData_Empty() throws Exception {
        // Arrange: no data
        when(chartDataService.getDailyChartData("FOO")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/charts/FOO/daily")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(chartDataService).getDailyChartData("FOO");
    }
}
