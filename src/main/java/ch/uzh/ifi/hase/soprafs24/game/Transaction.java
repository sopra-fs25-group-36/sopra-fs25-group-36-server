package ch.uzh.ifi.hase.soprafs24.game;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

//
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String stockId;
    private int quantity;
    private double price;
    private String type; // "BUY" or "SELL"

    public Transaction() {
    } // JPA requires a no-arg constructor

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
