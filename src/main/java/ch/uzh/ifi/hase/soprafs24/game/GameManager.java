package ch.uzh.ifi.hase.soprafs24.game;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors; // Kept from old version, might be useful

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;

public class GameManager {

    private static final Logger log = LoggerFactory.getLogger(GameManager.class);

    private final Long gameId;
    private final Map<Long, PlayerState> playerStates = new HashMap<>();
    private LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline; // Date -> (Symbol -> Price)

    private final long roundDelayMillis;
    private List<LeaderBoardEntry> leaderBoard = new ArrayList<>();
    private int currentRound = 1; // Game starts at round 1
    private boolean active = true;
    private final LocalDateTime startedAt = LocalDateTime.now();

    private static final long DEFAULT_ROUND_DELAY_MILLIS = 120_000; // 2 minutes
    private static final int MAX_ROUNDS = 10; // Define max rounds once
    private static final long SYNC_BUFFER_MILLIS = 2_000; // Buffer for synchronized round start
    private final ScheduledExecutorService scheduler;

    // private final ScheduledExecutorService roundScheduler;
    // private volatile boolean roundInProgress = false; // Flag to manage round
    // transition logic
    private long nextRoundStartTimeMillis = 0L; // For DTO, when next round might start due to all submissions
    private ScheduledFuture<?> nextRoundFuture;

    public GameManager(Long gameId, LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline,
            long roundDelayMillis) {
        this.gameId = gameId;
        this.stockTimeline = Objects.requireNonNull(stockTimeline, "Stock timeline cannot be null");
        if (stockTimeline.isEmpty()) {
            log.warn("GameManager for gameId {} initialized with an empty stock timeline.", gameId);
            // Consider throwing an IllegalArgumentException if an empty timeline is invalid
            // for game start
        }
        this.roundDelayMillis = roundDelayMillis;
        this.scheduler = Executors
                .newSingleThreadScheduledExecutor(r -> new Thread(r, "GameRoundScheduler-" + this.gameId));

        log.info("GameManager for gameId {} created. Round delay: {}ms. Timeline entries: {}.",
                gameId, roundDelayMillis, stockTimeline.size());
    }

    public GameManager(Long gameId, LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline) {
        this(gameId, stockTimeline, DEFAULT_ROUND_DELAY_MILLIS);
        log.info("GameManager for gameId {} created with default round delay: {}ms.", gameId,
                DEFAULT_ROUND_DELAY_MILLIS);
    }

    public void registerPlayer(Long userId) {
        if (playerStates.containsKey(userId)) {
            log.warn("Attempt to register duplicate player {}. Registration refused for game {}.", userId, gameId);
            throw new IllegalStateException("Player " + userId + " already exists in game " + gameId);
        }
        PlayerState ps = new PlayerState(userId);
        playerStates.put(userId, ps);
        log.info("Player {} registered for game {}. Total players: {}.", userId, gameId, playerStates.size());
        recalculateLeaderboard(); // Update leaderboard
    }

    public synchronized void submitTransactions(Long userId, List<TransactionRequestDTO> txs) {
        if (!active) {
            log.warn("Game {} is not active. Transactions from player {} rejected.", gameId, userId);
            return;
        }

        PlayerState state = playerStates.get(userId);
        if (state == null) {
            log.warn("Player {} not found in game {}. Transactions rejected.", userId, gameId);
            return;
        }

        if (state.hasSubmittedForRound(currentRound)) {
            log.warn("Player {} has already submitted for round {} in game {}. Additional transactions rejected.",
                    userId, currentRound, gameId);
            return;
        }

        Map<String, Double> pricesForTransaction = getCurrentStockPrices();
        if (pricesForTransaction.isEmpty() && txs.stream().anyMatch(tx -> !"INFO".equalsIgnoreCase(tx.getType()))) {
            log.error(
                    "CRITICAL: Cannot process buy/sell transactions for player {} in game {}: Current stock prices are unavailable for round {}.",
                    userId, gameId, currentRound);
            // Depending on game rules, you might throw an exception or prevent submission
            // entirely here.
            // For now, logging and allowing applyTransaction to potentially fail
            // internally.
            // throw new IllegalStateException("Stock prices unavailable for current round,
            // cannot process transactions.");
        }

        for (TransactionRequestDTO tx : txs) {
            state.applyTransaction(tx, pricesForTransaction);
        }
        state.markSubmittedForRound(currentRound);
        log.info("Player {} submitted {} transactions for round {} in game {}.", userId, txs.size(), currentRound,
                gameId);

        // Check if all active players have submitted
        boolean allSubmitted = haveAllPlayersSubmittedForCurrentRound();

        log.debug("Game {}: All submitted for round {}: {}. Round in progress flag: {}.", gameId, currentRound,
                allSubmitted);

        if (haveAllPlayersSubmittedForCurrentRound()) {
            // as soon as the last player in, throw out the old timer
            // and schedule a 2 s “all‐in” dispatch:
            scheduleNextRoundAfter(SYNC_BUFFER_MILLIS);
        }
    }

    public Map<String, Double> getCurrentStockPrices() {
        if (stockTimeline == null || stockTimeline.isEmpty()) {
            log.warn("Game {}: Stock timeline is null or empty. Cannot get current stock prices for round {}.", gameId,
                    currentRound);
            return Collections.emptyMap();
        }
        if (currentRound <= 0 || currentRound > stockTimeline.size()) {
            log.warn("Game {}: currentRound {} is out of bounds for stockTimeline size {}. Cannot get prices.", gameId,
                    currentRound, stockTimeline.size());
            return Collections.emptyMap();
        }

        List<Map<String, Double>> pricesByDate = new ArrayList<>(stockTimeline.values());
        Map<String, Double> pricesForCurrentRound = pricesByDate.get(currentRound - 1);

        return pricesForCurrentRound != null ? new HashMap<>(pricesForCurrentRound) : Collections.emptyMap();
    }

    // Added from your version - crucial for chart data
    public LocalDate getCurrentMarketDate() {
        if (stockTimeline == null || stockTimeline.isEmpty()) {
            log.warn("Game {}: Stock timeline is null or empty. Cannot get current market date for round {}.", gameId,
                    currentRound);
            return null;
        }
        if (currentRound <= 0 || currentRound > stockTimeline.size()) {
            log.warn(
                    "Game {}: currentRound {} is out of bounds for stockTimeline size {}. Cannot get current market date.",
                    gameId, currentRound, stockTimeline.size());
            return null;
        }

        List<LocalDate> datesInTimeline = new ArrayList<>(stockTimeline.keySet());
        LocalDate marketDateForCurrentRound = datesInTimeline.get(currentRound - 1);
        log.debug("Game {}: Current market date for round {} is {}.", gameId, currentRound, marketDateForCurrentRound);
        return marketDateForCurrentRound;
    }

    // Added from your version - useful for fetching historical data for charts
    public LocalDate getDateForRound(int round) {
        if (round <= 0) {
            log.warn("Game {}: Requested date for invalid round {}.", gameId, round);
            return null;
        }
        if (stockTimeline == null || stockTimeline.isEmpty() || round > stockTimeline.size()) {
            log.warn("Game {}: Round {} is out of bounds for stockTimeline size {}. Cannot get date.", gameId, round,
                    stockTimeline.size());
            return null;
        }
        List<LocalDate> datesInTimeline = new ArrayList<>(stockTimeline.keySet());
        return datesInTimeline.get(round - 1);
    }

    public List<LeaderBoardEntry> getLeaderBoard() {
        // Return a copy to prevent external modification
        return Collections.unmodifiableList(new ArrayList<>(leaderBoard));
    }

    public synchronized void nextRound() {
        if (!active) {
            log.info("Game {} is inactive. Not advancing to next round.", gameId);
            return;
        }
        for (PlayerState player : playerStates.values()) {
            player.snapshotHoldingsAtRound(currentRound);
        }
        if (currentRound < MAX_ROUNDS) {
            currentRound++;
            log.info("Game {}: Advanced to round {}.", gameId, currentRound);
            recalculateLeaderboard();
            // schedule the normal timeout for the new round
            scheduleNextRoundAfter(roundDelayMillis);

            // Player submission status implicitly reset by PlayerState checking against new
            // currentRound
        } else {
            log.info("Game {}: Max rounds ({}) reached. Ending game.", gameId, MAX_ROUNDS);
            endGame(); // endGame will also set active = false
        }
    }

    private void scheduleNextRoundAfter(long delay) {
        // cancel whatever was already pending
        if (nextRoundFuture != null && !nextRoundFuture.isDone()) {
            nextRoundFuture.cancel(false);
        }

        // record for DTO / front‐end
        nextRoundStartTimeMillis = System.currentTimeMillis() + delay;

        nextRoundFuture = scheduler.schedule(() -> {
            synchronized (GameManager.this) {
                if (!active)
                    return;
                nextRound();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public synchronized void startGame() {
        // Kick off the very first round’s timeout
        scheduleNextRoundAfter(roundDelayMillis);
    }

    private void recalculateLeaderboard() {
        List<LeaderBoardEntry> updatedBoard = new ArrayList<>();
        Map<String, Double> currentPricesForLeaderboard = getCurrentStockPrices(); // Get prices once

        for (PlayerState player : playerStates.values()) {
            double totalAssets = player.calculateTotalAssets(currentPricesForLeaderboard);
            log.debug("Game {}: Player {} ({}) total assets for leaderboard: {}", gameId, player.getUserId(),
                    totalAssets); // Assuming PlayerState might have a name/username field eventually for better
                                  // logs
            updatedBoard.add(new LeaderBoardEntry(player.getUserId(), totalAssets));
        }

        updatedBoard.sort((a, b) -> Double.compare(b.getTotalAssets(), a.getTotalAssets()));
        this.leaderBoard = updatedBoard; // Atomically update leaderboard

        if (!updatedBoard.isEmpty()) {
            log.info("Game {}: Leaderboard recalculated. Top player: {} with assets {}", gameId,
                    updatedBoard.get(0).getUserId(), updatedBoard.get(0).getTotalAssets());
        } else {
            log.info("Game {}: Leaderboard recalculated. No players on leaderboard.", gameId);
        }
    }

    public synchronized void endGame() {
        if (!this.active) {
            log.info("Game {} already ended.", gameId);
            return;
        }
        log.info("Game {} is ending. Final round was {}.", gameId, currentRound);
        this.active = false;
        recalculateLeaderboard(); // Final leaderboard calculation

        // 1) cancel the one outstanding “next round” job, if any
        if (nextRoundFuture != null && !nextRoundFuture.isDone()) {
            log.info("Game {}: Cancelling pending nextRound task.", gameId);
            nextRoundFuture.cancel(false);
        }

        // 2) shut down the ScheduledExecutorService
        log.info("Game {}: Shutting down round scheduler.", gameId);
        scheduler.shutdown();
        try {
            // wait up to 5 seconds for any running task to finish
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Game {}: Scheduler did not terminate in 5s; forcing shutdown.", gameId);
                scheduler.shutdownNow();
            } else {
                log.info("Game {}: Scheduler terminated cleanly.", gameId);
            }
        } catch (InterruptedException e) {
            log.warn("Game {}: Interrupted during scheduler shutdown; forcing shutdown.", gameId, e);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Game {} processing finished.", gameId);
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
        // Return an unmodifiable copy
        return Collections.unmodifiableMap(new HashMap<>(playerStates));
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LinkedHashMap<LocalDate, Map<String, Double>> getStockTimeline() {
        // Return a copy to prevent external modification of the game's timeline
        // instance
        return new LinkedHashMap<>(stockTimeline);
    }

    public void setStockTimeline(LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline) {
        this.stockTimeline = Objects.requireNonNull(stockTimeline, "Stock timeline cannot be set to null");
        log.info("Game {}: Stock timeline has been externally updated. New size: {}", gameId, stockTimeline.size());
    }

    public PlayerState getPlayerState(Long userId) {
        return playerStates.get(userId);
    }

    public boolean haveAllPlayersSubmittedForCurrentRound() {
        if (playerStates.isEmpty() && active) { // If game is active but no players
            return false; // No one to submit, so not "all submitted" in a meaningful way for progression
        }
        if (playerStates.isEmpty()) {
            return true; // Or false, depending on desired behavior for empty games. Let's say true for
                         // no one to wait for.
                         // However, typically a game wouldn't run without players or would auto-end.
                         // For safety, if empty & active, means we wait for players.
                         // Changed to false if active and empty, as per old code logic.
        }
        return playerStates.values().stream()
                .allMatch(player -> player.hasSubmittedForRound(currentRound));
    }

    public long getNextRoundStartTimeMillis() {
        return nextRoundStartTimeMillis;
    }
}