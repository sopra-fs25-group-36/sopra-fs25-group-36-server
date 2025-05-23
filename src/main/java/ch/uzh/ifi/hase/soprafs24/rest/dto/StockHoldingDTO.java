package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class StockHoldingDTO {
    private String symbol;
    private int quantity;
    private String category;
    private double currentPrice;

    public StockHoldingDTO() {
    }

    public StockHoldingDTO(String symbol, int quantity, String category, double currentPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.category = category;
        this.currentPrice = currentPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
}
