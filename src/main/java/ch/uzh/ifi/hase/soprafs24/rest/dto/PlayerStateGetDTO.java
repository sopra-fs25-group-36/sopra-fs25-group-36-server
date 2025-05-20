package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class PlayerStateGetDTO {

    private Long userId;
    private double cashBalance;

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

}
