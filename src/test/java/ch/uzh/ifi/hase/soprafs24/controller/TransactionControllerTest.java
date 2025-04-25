package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long gameId = 1L;
    private Long userId = 100L;

    @BeforeEach
    void setUp() {
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = new LinkedHashMap<>();
        Map<String, Double> daySnapshot = new HashMap<>();
        daySnapshot.put("AAPL", 150.0);
        timeline.put(LocalDate.now(), daySnapshot);

        GameManager gameManager = new GameManager(gameId, timeline);
        gameManager.registerPlayer(userId);

        InMemoryGameRegistry.registerGame(gameId, gameManager);
    }

    @AfterEach
    void tearDown() {
        InMemoryGameRegistry.clear();
    }


    @Test
    void submitTransaction_success() throws Exception {
        TransactionRequestDTO requestDTO = new TransactionRequestDTO();
        requestDTO.setStockId("AAPL");
        requestDTO.setQuantity(10);
        requestDTO.setType("BUY");

        mockMvc.perform(post("/api/transaction/{gameId}/submit", gameId)
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Transaction submitted."));
    }

    @Test
    void submitTransaction_gameNotFound() throws Exception {
        // Clear the registry to simulate missing game
        InMemoryGameRegistry.clear();

        TransactionRequestDTO requestDTO = new TransactionRequestDTO();
        requestDTO.setStockId("AAPL");
        requestDTO.setQuantity(10);
        requestDTO.setType("BUY");

        mockMvc.perform(post("/api/transaction/{gameId}/submit", gameId)
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Game not found."));
    }
}
