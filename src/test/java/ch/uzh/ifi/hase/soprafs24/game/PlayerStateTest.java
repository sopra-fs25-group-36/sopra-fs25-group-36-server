package ch.uzh.ifi.hase.soprafs24.game;


import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerStateTest {

    private PlayerState player;
    private Map<String, Double> prices;

    @BeforeEach
    public void setup() {
        player = new PlayerState(1L);
        prices = new HashMap<>();
        prices.put("AAPL", 100.0);
        prices.put("TSLA", 200.0);
    }

    @Test
    public void testBuyStock_validTransaction() {
        TransactionRequestDTO tx = new TransactionRequestDTO();
        tx.setStockId("AAPL");
        tx.setQuantity(10);
        tx.setType("BUY");

        player.applyTransaction(tx, prices);

        assertEquals(9000.0, player.getCashBalance(), 0.01);
        assertEquals(10, player.getPlayerStocks().get("AAPL"));
    }

    @Test
    public void testSellStock_validTransaction() {
        // Pre-load 5 shares
        TransactionRequestDTO buy = new TransactionRequestDTO();
        buy.setStockId("TSLA");
        buy.setQuantity(5);
        buy.setType("BUY");
        player.applyTransaction(buy, prices);

        TransactionRequestDTO sell = new TransactionRequestDTO();
        sell.setStockId("TSLA");
        sell.setQuantity(3);
        sell.setType("SELL");
        player.applyTransaction(sell, prices);

        assertEquals(10000.0 - 1000.0 + 600.0, player.getCashBalance(), 0.01);
        assertEquals(2, player.getPlayerStocks().get("TSLA"));
    }

    @Test
    public void testBuyStock_insufficientFunds() {
        TransactionRequestDTO tx = new TransactionRequestDTO();
        tx.setStockId("AAPL");
        tx.setQuantity(200); // 200 * 100 = 20,000 > 10,000
        tx.setType("BUY");

        player.applyTransaction(tx, prices);

        assertEquals(10000.0, player.getCashBalance(), 0.01);
        assertFalse(player.getPlayerStocks().containsKey("AAPL"));
    }

    @Test
    public void testSellStock_notEnoughShares() {
        TransactionRequestDTO tx = new TransactionRequestDTO();
        tx.setStockId("TSLA");
        tx.setQuantity(5);
        tx.setType("SELL");

        player.applyTransaction(tx, prices);

        assertEquals(10000.0, player.getCashBalance(), 0.01);
        assertFalse(player.getPlayerStocks().containsKey("TSLA"));
    }

    @Test
    public void testBuyStock_unknownSymbol() {
        TransactionRequestDTO tx = new TransactionRequestDTO();
        tx.setStockId("FAKE");
        tx.setQuantity(1);
        tx.setType("BUY");

        player.applyTransaction(tx, prices);
        System.out.println("Stocks owned" + player.getPlayerStocks());


        assertEquals(10000.0, player.getCashBalance(), 0.01);
        assertFalse(player.getPlayerStocks().containsKey("FAKE"));
    }
}
