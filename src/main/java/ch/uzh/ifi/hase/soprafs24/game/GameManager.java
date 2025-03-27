package ch.uzh.ifi.hase.soprafs24.game;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameManager {

    private final Long gameId;
    private final Map<Long, PlayerState> playerStates;
    private final List<Map<String, Double>> stockTimeline;
    private int currentRound;
    private boolean active;
    private final LocalDateTime startedAt;

    //for timing each round, 2mins
    private final ScheduledExecutorService roundScheduler = Executors.newSingleThreadScheduledExecutor();


    public GameManager(Long gameId, List<Map<String, Double>> stockTimeline) {
        this.gameId = gameId;
        this.stockTimeline = stockTimeline;
        this.currentRound = 1;
        this.active = true;
        this.startedAt = LocalDateTime.now();
        this.playerStates = new HashMap<>();
    }

    public void registerPlayer(Long userId) {
        playerStates.put(userId, new PlayerState(userId));
    }

    public void submitTransaction(Long userId, TransactionRequest tx) {
        PlayerState state = playerStates.get(userId);
        if (state != null && isActive()) {
            state.applyTransaction(tx, getCurrentStockPrices());
        }
    }

//    public Map<String, Double> getCurrentStockPrices() {
//        return stockTimeline.get(currentRound - 1);
//    }
    public Map<String, Double> getCurrentStockPrices() {
        // Return a mutable copy to avoid accidental immutability
        return new HashMap<>(stockTimeline.get(currentRound - 1));
}

//
    public void nextRound() {
        if (currentRound < 10) {
            currentRound++;
        } else {
            endGame();
        }
    }

    public void endGame() {
        this.active = false;
        InMemoryGameRegistry.remove(gameId);
    }

    public boolean isActive() {
        return active;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public Long getGameId() {
        return gameId;
    }

    public Map<Long, PlayerState> getPlayerStates() {
        return playerStates;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    public void scheduleRounds() {
        roundScheduler.scheduleAtFixedRate(() -> {
            if (!active) {
                roundScheduler.shutdown();
                return;
            }

            if (currentRound < 10) {
                System.out.println("Auto-progressing to round: " + (currentRound + 1));
                nextRound();
            } else {
                System.out.println("Game over, ending session.");
                endGame();
                roundScheduler.shutdown();
            }
        }, 5, 5, TimeUnit.MINUTES); // wait 5 min before first round, then every 5 min
    }




}