package ch.uzh.ifi.hase.soprafs24.game;
import java.util.List;
import java.util.ArrayList;

import ch.uzh.ifi.hase.soprafs24.game.Transaction;
import java.util.*;
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

    public void applyTransaction(TransactionRequest tx, Map<String, Double> currentPrices) {
        double price = currentPrices.getOrDefault(tx.getStockId(), 0.0);
        double total = price * tx.getQuantity();

        if ("BUY".equalsIgnoreCase(tx.getType()) && cashBalance >= total) {
            stocksOwned.put(tx.getStockId(), stocksOwned.getOrDefault(tx.getStockId(), 0) + tx.getQuantity());
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

    public double calculateNetWorth(Map<String, Double> prices) {
        double value = cashBalance;
        for (Map.Entry<String, Integer> entry : stocksOwned.entrySet()) {
            value += entry.getValue() * prices.getOrDefault(entry.getKey(), 0.0);
        }
        return value;
    }

    public double getCashBalance() {
        return cashBalance;
    }
}
