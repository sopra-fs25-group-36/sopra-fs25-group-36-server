package ch.uzh.ifi.hase.soprafs24.game;

public class LeaderBoardEntry {
    private Long userId;
    private double totalAssets;

    public LeaderBoardEntry(Long userId, double totalAssets) {
        this.userId = userId;
        this.totalAssets = totalAssets;
    }

    public Long getUserId() {
        return userId;
    }

    public double getTotalAssets() {
        return totalAssets;
    }
}