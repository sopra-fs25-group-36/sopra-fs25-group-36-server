package ch.uzh.ifi.hase.soprafs24.game;

import java.util.*;

import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;

// Current output in JSON

//{
//        "userId": 42,
//        "cashBalance": 5000,
//        "stocks": [
//        { "symbol": "AAPL", "quantity": 10, "category": "Tech", "currentPrice": 150 }
//        ],
//        "transactionHistory": [
//        { "stockId": "AAPL", "quantity": 5, "price": 140, "type": "BUY" },
//        { "stockId": "AAPL", "quantity": 3, "price": 145, "type": "BUY" }
//        ]
//        }

public class PlayerState {

    private final Long userId;
    private final Map<String, Integer> stocksOwned;
    private double cashBalance;
    private final List<Transaction> transactionHistory;

    private final Set<Integer> submittedRounds = new HashSet<>();


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

    public Map<String, Integer> getPlayerStocks() {
        return new HashMap<>(stocksOwned); // return a copy to prevent external modification
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
        if (!currentPrices.containsKey(tx.getStockId())) {
            return; // silently ignore or throw an exception
        }
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

    public void setStock(String stockId, int quantity) {
        stocksOwned.put(stockId, quantity);
    }
    public List<Transaction> getTransactionHistory() {
        return new ArrayList<>(transactionHistory); // or return Collections.unmodifiableList(...)
    }

    public boolean hasSubmittedForRound(int round) {
        return submittedRounds.contains(round);
    }

    public void markSubmittedForRound(int round) {
        submittedRounds.add(round);
    }

}
