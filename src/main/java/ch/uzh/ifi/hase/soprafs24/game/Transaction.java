package ch.uzh.ifi.hase.soprafs24.game;


public class Transaction {
    private String stockId;
    private int quantity;
    private double price;
    private String type; // "BUY" or "SELL"

    public Transaction(String stockId, int quantity, double price, String type) {
        this.stockId = stockId;
        this.quantity = quantity;
        this.price = price;
        this.type = type;
    }

    // Getters and setters (optional)
    public String getStockId() {
        return stockId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public String getType() {
        return type;
    }
}
