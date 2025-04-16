package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class PlayerStateGetDTO {

    private Long userId;
    private double cashBalance;
    private List<StockHoldingDTO> stocks;
    private List<TransactionRequestDTO> transactionHistory;

    // Getters and Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public List<StockHoldingDTO> getStocks() {
        return stocks;
    }

    public void setStocks(List<StockHoldingDTO> stocks) {
        this.stocks = stocks;
    }

    public List<TransactionRequestDTO> getTransactionHistory() {
        return transactionHistory;
    }

    public void setTransactionHistory(List<TransactionRequestDTO> transactionHistory) {
        this.transactionHistory = transactionHistory;
    }

}
