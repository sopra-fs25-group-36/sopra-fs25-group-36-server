package ch.uzh.ifi.hase.soprafs24.controller;

// Import static methods for MockMvc requests and results
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given; // BDD style mocking
import static org.mockito.Mockito.doNothing; // For void methods
import static org.mockito.Mockito.doThrow; // For exceptions
import static org.mockito.Mockito.verify; // To verify method calls
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // For checking response body
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito; // Import Mockito
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean; // Use MockBean for Spring context
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper; // Jackson for JSON conversion

import ch.uzh.ifi.hase.soprafs24.rest.dto.StockDataPointDTO;
import ch.uzh.ifi.hase.soprafs24.service.ChartDataService;

/**
 * Integration tests for the ChartDataController endpoints.
 * Mocks the ChartDataService to isolate controller logic.
 */
@WebMvcTest(ChartDataController.class) // Specify the controller to test
public class ChartDataControllerTest {

        @Autowired
        private MockMvc mockMvc; // Injects MockMvc instance for sending requests

        @MockBean // Creates a Mockito mock and registers it as a bean in the application context
        private ChartDataService chartDataService;

        @Autowired
        private ObjectMapper objectMapper; // For converting objects to JSON

        private StockDataPointDTO testDataPointDTO;

        @BeforeEach
        void setUp() {
                // Reset mocks before each test if needed (often handled by Spring
                // automatically)
                // Mockito.reset(chartDataService);

                // Setup common test data
                testDataPointDTO = new StockDataPointDTO(
                                "AAPL",
                                LocalDate.of(2024, 5, 1),
                                170.0, 175.0, 169.0, 174.5, 100000L // open, high, low, close, volume
                );
        }

        // --- Tests for POST /api/charts/fetch-daily ---

        @Test
        public void whenPostFetchDaily_thenServiceIsCalledAndStatusAccepted() throws Exception {
                // Arrange: Mock the service method (it's void, so use doNothing)
                doNothing().when(chartDataService).fetchAndStoreDailyDataForAllSymbols();

                // Act: Perform the POST request
                MockHttpServletRequestBuilder postRequest = post("/api/charts/fetch-daily")
                                .contentType(MediaType.APPLICATION_JSON);

                // Assert
                mockMvc.perform(postRequest)
                                .andExpect(status().isAccepted()) // Expect 202 Accepted
                                .andExpect(content().string("Daily chart data fetching process started successfully."));

                // Verify that the service method was called exactly once
                verify(chartDataService, Mockito.times(1)).fetchAndStoreDailyDataForAllSymbols();
        }

        @Test
        public void whenPostFetchDailyAndServiceThrowsException_thenStatusInternalServerError() throws Exception {
                // Arrange: Mock the service method to throw an exception
                String errorMessage = "API limit reached";
                doThrow(new RuntimeException(errorMessage)).when(chartDataService)
                                .fetchAndStoreDailyDataForAllSymbols();

                // Act: Perform the POST request
                MockHttpServletRequestBuilder postRequest = post("/api/charts/fetch-daily")
                                .contentType(MediaType.APPLICATION_JSON);

                // Assert
                mockMvc.perform(postRequest)
                                .andExpect(status().isInternalServerError()) // Expect 500 Internal Server Error
                                .andExpect(content().string("Failed to start data fetching: " + errorMessage));

                // Verify that the service method was still called exactly once
                verify(chartDataService, Mockito.times(1)).fetchAndStoreDailyDataForAllSymbols();
        }

        // --- Tests for GET /api/charts/{symbol}/daily ---

        @Test
        public void whenGetDailyChartDataWithValidSymbolAndDataExists_thenReturnDataAndStatusOK() throws Exception {
                // Arrange: Mock the service to return a list with our test DTO
                List<StockDataPointDTO> dataList = Collections.singletonList(testDataPointDTO);
                given(chartDataService.getDailyChartData("AAPL")).willReturn(dataList);

                // Act: Perform the GET request
                MockHttpServletRequestBuilder getRequest = get("/api/charts/AAPL/daily")
                                .accept(MediaType.APPLICATION_JSON); // Expect JSON response

                // Assert
                mockMvc.perform(getRequest)
                                .andExpect(status().isOk()) // Expect 200 OK
                                .andExpect(jsonPath("$", hasSize(1))) // Expect an array of size 1
                                .andExpect(jsonPath("$[0].symbol", is("AAPL")))
                                .andExpect(jsonPath("$[0].date", is("2024-05-01")))
                                .andExpect(jsonPath("$[0].close", is(174.5)))
                                .andExpect(jsonPath("$[0].volume", is(100000)));

                // Verify service call
                verify(chartDataService, Mockito.times(1)).getDailyChartData("AAPL");
        }

        @Test
        public void whenGetDailyChartDataWithValidSymbolAndNoDataExists_thenReturnEmptyListAndStatusOK()
                        throws Exception {
                // Arrange: Mock the service to return an empty list
                given(chartDataService.getDailyChartData("MSFT")).willReturn(Collections.emptyList());

                // Act: Perform the GET request
                MockHttpServletRequestBuilder getRequest = get("/api/charts/MSFT/daily")
                                .accept(MediaType.APPLICATION_JSON);

                // Assert
                mockMvc.perform(getRequest)
                                .andExpect(status().isOk()) // Still 200 OK
                                .andExpect(jsonPath("$", hasSize(0))); // Expect an empty array

                // Verify service call
                verify(chartDataService, Mockito.times(1)).getDailyChartData("MSFT");
        }

        @Test
        public void whenGetDailyChartDataWithCaseInsensitiveSymbol_thenUsesUppercaseAndReturnsData() throws Exception {
                // Arrange: Mock the service to return data only for the uppercase version
                List<StockDataPointDTO> dataList = Collections.singletonList(testDataPointDTO); // testDataPointDTO is
                                                                                                // AAPL
                given(chartDataService.getDailyChartData("AAPL")).willReturn(dataList);
                given(chartDataService.getDailyChartData("aapl")).willReturn(Collections.emptyList()); // Mock lowercase
                                                                                                       // call if
                                                                                                       // needed, though
                                                                                                       // controller
                                                                                                       // should
                                                                                                       // uppercase

                // Act: Perform the GET request with lowercase symbol
                MockHttpServletRequestBuilder getRequest = get("/api/charts/aapl/daily")
                                .accept(MediaType.APPLICATION_JSON);

                // Assert: Should still find data because controller uppercases before calling
                // service
                mockMvc.perform(getRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].symbol", is("AAPL"))); // Check symbol in response

                // Verify the service was called with the UPPERCASE symbol
                verify(chartDataService, Mockito.times(1)).getDailyChartData("AAPL");
                // Optionally verify lowercase wasn't called if you added that mock:
                // verify(chartDataService, Mockito.never()).getDailyChartData("aapl");
        }

}
