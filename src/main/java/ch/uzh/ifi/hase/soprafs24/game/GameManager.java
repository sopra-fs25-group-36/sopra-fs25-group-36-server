package ch.uzh.ifi.hase.soprafs24.game;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;

public class GameManager {

    private final Long gameId;
    private final Map<Long, PlayerState> playerStates = new HashMap<>();
    private LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline;

    private final long roundDelayMillis;
    private List<LeaderBoardEntry> leaderBoard = new ArrayList<>();
    private int currentRound = 1;
    private boolean active = true;
    private final LocalDateTime startedAt = LocalDateTime.now();

    private static final long DEFAULT_ROUND_DELAY_MILLIS = 180_000; // 2 minutes
    private final ScheduledExecutorService roundScheduler;

    //  Main constructor with round delay override
    public GameManager(Long gameId, LinkedHashMap<LocalDate, Map<String, Double>>  stockTimeline, long roundDelayMillis) {
        this.gameId = gameId;
        this.stockTimeline = stockTimeline;
        this.roundDelayMillis = roundDelayMillis;
        this.roundScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    // Production constructor with default round delay
    public GameManager(Long gameId, LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline) {
        this(gameId, stockTimeline, DEFAULT_ROUND_DELAY_MILLIS);
    }

    public void registerPlayer(Long userId) {
        playerStates.put(userId, new PlayerState(userId));
        recalculateLeaderboard(); // Update leaderboard after adding a new player
    }

    public void submitTransaction(Long userId, TransactionRequestDTO tx) {
        PlayerState state = playerStates.get(userId);
        if (state != null && isActive()) {
            state.applyTransaction(tx, getCurrentStockPrices());
        }
    }

    public Map<String, Double> getCurrentStockPrices() {
        if (stockTimeline == null || stockTimeline.isEmpty() || currentRound <= 0 || currentRound > stockTimeline.size()) {
            return new HashMap<>();
        }

        List<Map<String, Double>> rounds = new ArrayList<>(stockTimeline.values());
        Map<String, Double> prices = rounds.get(currentRound - 1);
        return prices != null ? new HashMap<>(prices) : new HashMap<>();
    }

    public List<LeaderBoardEntry> getLeaderBoard() {
        return leaderBoard;
    }

    public void nextRound() {
        recalculateLeaderboard();
        if (currentRound < 10) {
            currentRound++;
        } else {
            endGame();
        }
    }

    private void recalculateLeaderboard() {
        List<LeaderBoardEntry> updatedBoard = new ArrayList<>();
        for (Map.Entry<Long, PlayerState> entry : playerStates.entrySet()) {
            Long userId = entry.getKey();
            PlayerState player = entry.getValue();
            double total = player.calculateTotalAssets(getCurrentStockPrices());
            updatedBoard.add(new LeaderBoardEntry(userId, total));
        }

        updatedBoard.sort((a, b) -> Double.compare(b.getTotalAssets(), a.getTotalAssets())); // RANKING
        this.leaderBoard = updatedBoard;
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
        }, roundDelayMillis, roundDelayMillis, TimeUnit.MILLISECONDS);
    }

    public LinkedHashMap<LocalDate, Map<String, Double>> getStockTimeline() {
        return stockTimeline;
    }

    public void setStockTimeline(LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline) {
        this.stockTimeline = stockTimeline;
    }

    public PlayerState getPlayerState(Long userId) { return playerStates.get(userId);
    }
}