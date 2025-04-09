package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LeaderBoardEntryGetDTO {

    private Long userId;
    private double totalAssets;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public double getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(double totalAssets) {
        this.totalAssets = totalAssets;
    }
}
