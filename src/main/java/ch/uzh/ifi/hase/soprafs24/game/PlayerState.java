package ch.uzh.ifi.hase.soprafs24.game;

import java.util.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;

public class PlayerState {

    private final Long userId;
    private final Map<String, Integer> stocksOwned;
    private double cashBalance;
    private final List<Transaction> transactionHistory;

    private final Set<Integer> submittedRounds = new HashSet<>();
    private final Map<Integer, Map<String, Integer>> stockHistory = new HashMap<>();

    public PlayerState(Long userId) {
        this.userId = userId;
        this.stocksOwned = new HashMap<>();
        this.cashBalance = 10000.0;
        this.transactionHistory = new ArrayList<>();
    }

    public Long getUserId() {
        return userId;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public Map<String, Integer> getPlayerStocks() {
        return new HashMap<>(stocksOwned);
    }

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

    public void applyTransaction(TransactionRequestDTO tx, Map<String, Double> currentPrices) {
        if (!currentPrices.containsKey(tx.getStockId())) {
            return;
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
        return new ArrayList<>(transactionHistory);
    }

    public boolean hasSubmittedForRound(int round) {
        return submittedRounds.contains(round);
    }

    public void markSubmittedForRound(int round) {
        submittedRounds.add(round);
    }

    public double getTotalAssets(Map<String, Double> prices) {
        return calculateTotalAssets(prices);
    }

    public void snapshotHoldingsAtRound(int round) {
        if (stocksOwned == null)
            return;
        stockHistory.put(round, new HashMap<>(stocksOwned));
    }

    public Map<String, Integer> getHoldingsForRound(int round) {
        return stockHistory.getOrDefault(round, Collections.emptyMap());
    }

}
