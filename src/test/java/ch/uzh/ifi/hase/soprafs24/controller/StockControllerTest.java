package ch.uzh.ifi.hase.soprafs24.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.StockService;

@WebMvcTest(StockController.class)
public class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    private List<StockPriceGetDTO> mockStockPrices;
    private List<StockHoldingDTO> mockHoldings;
    private Map<String, String> mockCategories;

    @BeforeEach
    public void setup() {
        // Setup mock stock prices
        mockStockPrices = Arrays.asList(
                createStockPriceDTO("AAPL", 150.25),
                createStockPriceDTO("MSFT", 320.75),
                createStockPriceDTO("TSLA", 450.00));

        // Setup mock holdings
        mockHoldings = Arrays.asList(
                createStockHoldingDTO("AAPL", 10),
                createStockHoldingDTO("TSLA", 5));

        // Setup mock categories
        mockCategories = new HashMap<>();
        mockCategories.put("AAPL", "Technology");
        mockCategories.put("MSFT", "Technology");
        mockCategories.put("TSLA", "Automotive");
    }

    private StockPriceGetDTO createStockPriceDTO(String symbol, double price) {
        StockPriceGetDTO dto = new StockPriceGetDTO();
        dto.setSymbol(symbol);
        dto.setPrice(price);
        return dto;
    }

    private StockHoldingDTO createStockHoldingDTO(String symbol, int quantity) {
        StockHoldingDTO dto = new StockHoldingDTO();
        dto.setSymbol(symbol);
        dto.setQuantity(quantity);
        return dto;
    }

    @Test
    public void testFetchStaticPopularSymbols_Success() throws Exception {
        // Given
        doNothing().when(stockService).fetchKnownPopularStocks();

        // When/Then
        mockMvc.perform(post("/api/stocks/fetch/popular-static")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Fetched and saved data for known popular stocks."));

        verify(stockService, times(1)).fetchKnownPopularStocks();
    }

    @Test
    public void testGetCategories_ReturnsAllCategories() throws Exception {
        // Given
        when(stockService.getCategoryMap()).thenReturn(mockCategories);

        // When/Then
        mockMvc.perform(get("/api/stocks/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.AAPL", is("Technology")))
                .andExpect(jsonPath("$.MSFT", is("Technology")))
                .andExpect(jsonPath("$.TSLA", is("Automotive")));

        verify(stockService, times(1)).getCategoryMap();
    }

    @Test
    public void testGetPlayerHoldings_ReturnsCorrectHoldings() throws Exception {
        // Given
        Long userId = 1L;
        Long gameId = 2L;
        when(stockService.getPlayerHoldings(userId, gameId)).thenReturn(mockHoldings);

        // When/Then
        mockMvc.perform(get("/api/stocks/player-holdings/{userId}", userId)
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].symbol", is("AAPL")))
                .andExpect(jsonPath("$[0].quantity", is(10)))
                .andExpect(jsonPath("$[1].symbol", is("TSLA")))
                .andExpect(jsonPath("$[1].quantity", is(5)));

        verify(stockService, times(1)).getPlayerHoldings(userId, gameId);
    }

    @Test
    public void testGetPrice_WithoutParams_ReturnsCurrentRoundPrices() throws Exception {
        // Given
        Long gameId = 3L;
        when(stockService.getCurrentRoundStockPrices(gameId)).thenReturn(mockStockPrices);

        // When/Then
        mockMvc.perform(get("/api/stocks/{gameId}/stocks", gameId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].symbol", is("AAPL")))
                .andExpect(jsonPath("$[0].price", is(150.25)))
                .andExpect(jsonPath("$[1].symbol", is("MSFT")))
                .andExpect(jsonPath("$[2].symbol", is("TSLA")));

        verify(stockService, times(1)).getCurrentRoundStockPrices(gameId);
        verify(stockService, never()).getStockPrice(any(), any(), any());
    }

    @Test
    public void testGetPrice_WithParams_ReturnsSpecificStockPrice() throws Exception {
        // Given
        Long gameId = 3L;
        String symbol = "AAPL";
        Integer round = 2;
        List<StockPriceGetDTO> singleStockPrice = List.of(createStockPriceDTO("AAPL", 145.50));

        when(stockService.getStockPrice(gameId, symbol, round)).thenReturn(singleStockPrice);

        // When/Then
        mockMvc.perform(get("/api/stocks/{gameId}/stocks", gameId)
                .param("symbol", symbol)
                .param("round", round.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol", is("AAPL")))
                .andExpect(jsonPath("$[0].price", is(145.50)));

        verify(stockService, times(1)).getStockPrice(gameId, symbol, round);
        verify(stockService, never()).getCurrentRoundStockPrices(any());
    }

    @Test
    public void testGetAllStockData_ReturnsCompleteData() throws Exception {
        // Given
        Long userId = 4L;
        Long gameId = 5L;
        when(stockService.getCategoryMap()).thenReturn(mockCategories);
        when(stockService.getPlayerHoldings(userId, gameId)).thenReturn(mockHoldings);

        // When/Then
        mockMvc.perform(get("/api/stocks/all-data/{userId}", userId)
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.AAPL", is("Technology")))
                .andExpect(jsonPath("$.categories.MSFT", is("Technology")))
                .andExpect(jsonPath("$.categories.TSLA", is("Automotive")))
                .andExpect(jsonPath("$.holdings", hasSize(2)))
                .andExpect(jsonPath("$.holdings[0].symbol", is("AAPL")))
                .andExpect(jsonPath("$.holdings[1].symbol", is("TSLA")));

        verify(stockService, times(1)).getCategoryMap();
        verify(stockService, times(1)).getPlayerHoldings(userId, gameId);
    }
}
