package ch.uzh.ifi.hase.soprafs24.game;

public class TransactionRequest {
    private String stockId;
    private int quantity;
    private String type; // BUY or SELL

    // Getters and Setters
    public String getStockId() {
        return stockId;
    }
    public void setStockId(String stockId) {
        this.stockId = stockId;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}