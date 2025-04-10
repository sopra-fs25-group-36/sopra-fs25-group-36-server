//SJ: game package is centered on game logic (GameManager, PlayerState, Transaction)
//DTOs should be data containers for HTTP communication 
//This is not a class that does the actual game logic,transaction request that the client (front) sends to the backend. so i thought it is more right to put it in the dto folder. 
package ch.uzh.ifi.hase.soprafs24.rest.dto;


public class TransactionRequestDTO {
    private String stockId;
    private int quantity;
    private String type; // BUY or SELL
    private double price;

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

    public double getStockPrice(){return price; }

    public void setStockPrice(double price){this.price=price;}

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}
