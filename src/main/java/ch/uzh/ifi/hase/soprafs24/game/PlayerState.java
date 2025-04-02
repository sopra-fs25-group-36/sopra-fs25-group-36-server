package ch.uzh.ifi.hase.soprafs24.game;

import java.util.*;

import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;

public class PlayerState {

    private final Long userId;
    private final Map<String, Integer> stocksOwned;
    private double cashBalance;
    private final List<Transaction> transactionHistory;

    public PlayerState(Long userId) {
        this.userId = userId;
        this.stocksOwned = new HashMap<>();
        this.cashBalance = 10000.0; // initial cash
        this.transactionHistory = new ArrayList<>();
    }

    public Long getUserId() {
        return userId;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    // Calculates the total assets of the player based on current stock prices.

    public double calculateTotalAssets(Map<String, Double> stockPrices) {
        double total = cashBalance;
        for (Map.Entry<String, Integer> entry : stocksOwned.entrySet()) {
            String symbol = entry.getKey();
            int quantity = entry.getValue();
            double price = stockPrices.getOrDefault(symbol, 0.0);
            total += quantity * price;
        }
        return total;
    }

    // Applies a transaction to the player's portfolio.

    public void applyTransaction(TransactionRequestDTO tx, Map<String, Double> currentPrices) {
        double price = currentPrices.getOrDefault(tx.getStockId(), 0.0);
        double total = price * tx.getQuantity();

        if ("BUY".equalsIgnoreCase(tx.getType()) && cashBalance >= total) {
            stocksOwned.put(tx.getStockId(),
                    stocksOwned.getOrDefault(tx.getStockId(), 0) + tx.getQuantity());
            cashBalance -= total;
            transactionHistory.add(new Transaction(tx.getStockId(), tx.getQuantity(), price, "BUY"));
        } else if ("SELL".equalsIgnoreCase(tx.getType())) {
            int current = stocksOwned.getOrDefault(tx.getStockId(), 0);
            if (current >= tx.getQuantity()) {
                stocksOwned.put(tx.getStockId(), current - tx.getQuantity());
                cashBalance += total;
                transactionHistory.add(new Transaction(tx.getStockId(), tx.getQuantity(), price, "SELL"));
            }
        }
    }
}
