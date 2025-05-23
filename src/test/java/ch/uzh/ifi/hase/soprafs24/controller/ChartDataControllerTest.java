package ch.uzh.ifi.hase.soprafs24.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockDataPointDTO;
import ch.uzh.ifi.hase.soprafs24.service.ChartDataService;

@WebMvcTest(ChartDataController.class)
public class ChartDataControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ChartDataService chartDataService;

        @Autowired
        private ObjectMapper objectMapper;

        private StockDataPointDTO testDataPointDTO;

        @BeforeEach
        void setUp() {
                testDataPointDTO = new StockDataPointDTO(
                                "AAPL",
                                LocalDate.of(2024, 5, 1),
                                170.0, 175.0, 169.0, 174.5, 100000L);
        }

        @Test
        public void whenPostFetchDaily_thenServiceIsCalledAndStatusAccepted() throws Exception {
                doNothing().when(chartDataService).fetchAndStoreDailyDataForAllSymbols();
                MockHttpServletRequestBuilder postRequest = post("/api/charts/fetch-daily")
                                .contentType(MediaType.APPLICATION_JSON);
                mockMvc.perform(postRequest)
                                .andExpect(status().isAccepted())
                                .andExpect(content().string("Daily chart data fetching process started successfully."));
                verify(chartDataService, Mockito.times(1)).fetchAndStoreDailyDataForAllSymbols();
        }

        @Test
        public void whenPostFetchDailyAndServiceThrowsException_thenStatusInternalServerError() throws Exception {
                String errorMessage = "API limit reached";
                doThrow(new RuntimeException(errorMessage)).when(chartDataService)
                                .fetchAndStoreDailyDataForAllSymbols();
                MockHttpServletRequestBuilder postRequest = post("/api/charts/fetch-daily")
                                .contentType(MediaType.APPLICATION_JSON);
                mockMvc.perform(postRequest)
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string("Failed to start data fetching: " + errorMessage));

                verify(chartDataService, Mockito.times(1)).fetchAndStoreDailyDataForAllSymbols();
        }

        @Test
        public void whenGetDailyChartDataWithValidSymbolAndDataExists_thenReturnDataAndStatusOK() throws Exception {
                List<StockDataPointDTO> dataList = Collections.singletonList(testDataPointDTO);
                given(chartDataService.getDailyChartData("AAPL")).willReturn(dataList);
                MockHttpServletRequestBuilder getRequest = get("/api/charts/AAPL/daily")
                                .accept(MediaType.APPLICATION_JSON);
                mockMvc.perform(getRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].symbol", is("AAPL")))
                                .andExpect(jsonPath("$[0].date", is("2024-05-01")))
                                .andExpect(jsonPath("$[0].close", is(174.5)))
                                .andExpect(jsonPath("$[0].volume", is(100000)));
                verify(chartDataService, Mockito.times(1)).getDailyChartData("AAPL");
        }

        @Test
        public void whenGetDailyChartDataWithValidSymbolAndNoDataExists_thenReturnEmptyListAndStatusOK()
                        throws Exception {
                given(chartDataService.getDailyChartData("MSFT")).willReturn(Collections.emptyList());
                MockHttpServletRequestBuilder getRequest = get("/api/charts/MSFT/daily")
                                .accept(MediaType.APPLICATION_JSON);
                mockMvc.perform(getRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
                verify(chartDataService, Mockito.times(1)).getDailyChartData("MSFT");
        }

        @Test
        public void whenGetDailyChartDataWithCaseInsensitiveSymbol_thenUsesUppercaseAndReturnsData() throws Exception {
                List<StockDataPointDTO> dataList = Collections.singletonList(testDataPointDTO);
                given(chartDataService.getDailyChartData("AAPL")).willReturn(dataList);
                given(chartDataService.getDailyChartData("aapl")).willReturn(Collections.emptyList());
                MockHttpServletRequestBuilder getRequest = get("/api/charts/aapl/daily")
                                .accept(MediaType.APPLICATION_JSON);
                mockMvc.perform(getRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].symbol", is("AAPL")));
                verify(chartDataService, Mockito.times(1)).getDailyChartData("AAPL");
        }
}
