package ch.uzh.ifi.hase.soprafs24.game;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors; // Added for potential future use or cleaner syntax

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

    private final ScheduledExecutorService roundScheduler;
    private volatile boolean roundInProgress = false; // Flag to manage round transition logic

    public GameManager(Long gameId, LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline, long roundDelayMillis) {
        this.gameId = gameId;
        this.stockTimeline = Objects.requireNonNull(stockTimeline, "Stock timeline cannot be null");
        if (stockTimeline.isEmpty()) {
            log.warn("GameManager for gameId {} initialized with an empty stock timeline.", gameId);
            // Consider throwing an IllegalArgumentException if an empty timeline is invalid for game start
        }
        this.roundDelayMillis = roundDelayMillis;
        this.roundScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("GameRoundScheduler-" + gameId);
            return thread;
        });
        log.info("GameManager for gameId {} created. Round delay: {}ms. Timeline entries: {}.",
                gameId, roundDelayMillis, stockTimeline.size());
    }

    public GameManager(Long gameId, LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline) {
        this(gameId, stockTimeline, DEFAULT_ROUND_DELAY_MILLIS);
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
            // Optionally throw an exception or return a specific status
            return;
        }

        PlayerState state = playerStates.get(userId);
        if (state == null) {
            log.warn("Player {} not found in game {}. Transactions rejected.", userId, gameId);
            // Optionally throw an exception
            return;
        }

        if (state.hasSubmittedForRound(currentRound)) {
            log.warn("Player {} has already submitted for round {} in game {}. Additional transactions rejected.", userId, currentRound, gameId);
            // Optionally throw an exception
            return;
        }

        Map<String, Double> pricesForTransaction = getCurrentStockPrices();
        if (pricesForTransaction.isEmpty() && txs.stream().anyMatch(tx -> !"INFO".equalsIgnoreCase(tx.getType()))) { // Assuming INFO type doesn't need prices
            log.error("CRITICAL: Cannot process buy/sell transactions for player {} in game {}: Current stock prices are unavailable for round {}.", userId, gameId, currentRound);
            // This is a severe issue, might indicate a problem with stockTimeline or currentRound logic.
            // Throw an exception or prevent submission.
            // For now, we'll log and let it attempt to proceed, but applyTransaction might fail.
            // throw new IllegalStateException("Stock prices unavailable for current round, cannot process transactions.");
        }

        for (TransactionRequestDTO tx : txs) {
            state.applyTransaction(tx, pricesForTransaction);
        }
        state.markSubmittedForRound(currentRound);
        log.info("Player {} submitted {} transactions for round {} in game {}.", userId, txs.size(), currentRound, gameId);

        // Check if all active players have submitted
        boolean allSubmitted = playerStates.values().stream()
                .allMatch(ps -> ps.hasSubmittedForRound(currentRound));

        log.debug("Game {}: All submitted for round {}: {}. Round in progress flag: {}.", gameId, currentRound, allSubmitted, roundInProgress);

        if (allSubmitted && !roundInProgress) {
            this.roundInProgress = true; // Set flag to prevent re-entry
            log.info("Game {}: All players have submitted for round {}. Advancing to next round.", gameId, currentRound);
            // Consider a small delay or async execution for nextRound if it's long
            nextRound();
        }
    }

    public Map<String, Double> getCurrentStockPrices() {
        if (stockTimeline == null || stockTimeline.isEmpty()) {
            log.warn("Game {}: Stock timeline is null or empty. Cannot get current stock prices for round {}.", gameId, currentRound);
            return Collections.emptyMap();
        }
        if (currentRound <= 0 || currentRound > stockTimeline.size()) {
            log.warn("Game {}: currentRound {} is out of bounds for stockTimeline size {}. Cannot get prices.", gameId, currentRound, stockTimeline.size());
            return Collections.emptyMap();
        }

        List<Map<String, Double>> pricesByDate = new ArrayList<>(stockTimeline.values());
        Map<String, Double> pricesForCurrentRound = pricesByDate.get(currentRound - 1);

        return pricesForCurrentRound != null ? new HashMap<>(pricesForCurrentRound) : Collections.emptyMap();
    }

    public LocalDate getCurrentMarketDate() {
        if (stockTimeline == null || stockTimeline.isEmpty()) {
            log.warn("Game {}: Stock timeline is null or empty. Cannot get current market date for round {}.", gameId, currentRound);
            return null;
        }
        if (currentRound <= 0 || currentRound > stockTimeline.size()) {
            log.warn("Game {}: currentRound {} is out of bounds for stockTimeline size {}. Cannot get current market date.", gameId, currentRound, stockTimeline.size());
            return null;
        }

        List<LocalDate> datesInTimeline = new ArrayList<>(stockTimeline.keySet());
        LocalDate marketDateForCurrentRound = datesInTimeline.get(currentRound - 1);
        log.debug("Game {}: Current market date for round {} is {}.", gameId, currentRound, marketDateForCurrentRound);
        return marketDateForCurrentRound;
    }

    public LocalDate getDateForRound(int round) {
        if (round <= 0) {
            log.warn("Game {}: Requested date for invalid round {}.", gameId, round);
            return null;
        }
        if (stockTimeline == null || stockTimeline.isEmpty() || round > stockTimeline.size()) {
            log.warn("Game {}: Round {} is out of bounds for stockTimeline size {}. Cannot get date.", gameId, round, stockTimeline.size());
            return null;
        }
        List<LocalDate> datesInTimeline = new ArrayList<>(stockTimeline.keySet());
        return datesInTimeline.get(round - 1);
    }

    public List<LeaderBoardEntry> getLeaderBoard() {
        return Collections.unmodifiableList(new ArrayList<>(leaderBoard)); // Return a copy
    }

    public synchronized void nextRound() {
        if (!active) {
            log.info("Game {} is inactive. Not advancing to next round.", gameId);
            this.roundInProgress = false; // Ensure flag is reset if called while inactive
            return;
        }

        if (currentRound < MAX_ROUNDS) {
            currentRound++;
            log.info("Game {}: Advanced to round {}.", gameId, currentRound);
            // Player submission status is implicitly handled by checking against the new currentRound
            // in hasSubmittedForRound(currentRound) in PlayerState.
            recalculateLeaderboard();
        } else {
            log.info("Game {}: Max rounds ({}) reached. Ending game.", gameId, MAX_ROUNDS);
            endGame(); // endGame will also set active = false
        }
        this.roundInProgress = false; // Reset flag: new round is ready for submissions or next auto-advance
    }

    private void recalculateLeaderboard() {
        List<LeaderBoardEntry> updatedBoard = new ArrayList<>();
        Map<String, Double> currentPricesForLeaderboard = getCurrentStockPrices(); // Get prices once

        for (PlayerState player : playerStates.values()) {
            double totalAssets = player.calculateTotalAssets(currentPricesForLeaderboard);
            log.debug("Game {}: Player {} total assets for leaderboard: {}", gameId, player.getUserId(), totalAssets);
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

        if (roundScheduler != null && !roundScheduler.isShutdown()) {
            log.info("Game {}: Shutting down round scheduler.", gameId);
            roundScheduler.shutdown();
            try {
                if (!roundScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Game {}: Round scheduler did not terminate in 5 seconds. Forcing shutdown.", gameId);
                    roundScheduler.shutdownNow();
                } else {
                    log.info("Game {}: Round scheduler terminated gracefully.", gameId);
                }
            } catch (InterruptedException e) {
                log.warn("Game {}: Interrupted while waiting for round scheduler to terminate.", gameId, e);
                roundScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Game {} processing finished.", gameId);
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
        return Collections.unmodifiableMap(new HashMap<>(playerStates)); // Return a copy
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void scheduleRounds() {
        if (roundScheduler.isShutdown() || roundScheduler.isTerminated()) {
            log.warn("Game {}: Round scheduler is already shutdown. Cannot schedule rounds.", gameId);
            return;
        }
        log.info("Game {}: Scheduling automatic round progression every {} ms.", gameId, roundDelayMillis);
        roundScheduler.scheduleAtFixedRate(() -> {
            try {
                synchronized (GameManager.this) { // Synchronize on GameManager instance
                    if (!active) {
                        log.info("Game {}: Game inactive, stopping scheduled round progression.", gameId);
                        if (!roundScheduler.isShutdown()) roundScheduler.shutdown();
                        return;
                    }
                    if (roundInProgress) {
                        log.debug("Game {}: Round {} is already in progress (likely due to all players submitting). Skipping scheduled advancement.", gameId, currentRound);
                        return;
                    }

                    log.info("Game {}: Scheduled task triggered for round {}. Auto-progressing.", gameId, currentRound);
                    this.roundInProgress = true; // Set flag before calling nextRound
                    nextRound(); // nextRound will reset roundInProgress
                }
            } catch (Exception e) {
                log.error("Game {}: Unhandled exception in scheduled round progression task: {}", gameId, e.getMessage(), e);
                // Consider shutting down scheduler on critical errors to prevent repeated failures
                // if (!roundScheduler.isShutdown()) roundScheduler.shutdown();
            }
        }, roundDelayMillis, roundDelayMillis, TimeUnit.MILLISECONDS);
    }

    public LinkedHashMap<LocalDate, Map<String, Double>> getStockTimeline() {
        // Returning a direct reference allows modification. Return a copy if immutability is desired.
        return new LinkedHashMap<>(stockTimeline);
    }

    // Generally, stock timeline should be immutable after game start.
    // If modification is needed, ensure it's handled carefully (e.g., for testing).
    public void setStockTimeline(LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline) {
        this.stockTimeline = Objects.requireNonNull(stockTimeline, "Stock timeline cannot be set to null");
        log.info("Game {}: Stock timeline has been externally updated. New size: {}", gameId, stockTimeline.size());
    }

    public PlayerState getPlayerState(Long userId) {
        return playerStates.get(userId);
    }

    public boolean haveAllPlayersSubmittedForCurrentRound() {
        if (playerStates.isEmpty() && active) { // If game is active but no players, technically none have submitted
            return false;
        }
        if (playerStates.isEmpty()) return true; // If no players, arguably "all" (zero) have submitted.

        return playerStates.values().stream()
                .allMatch(player -> player.hasSubmittedForRound(currentRound));
    }

    // Getter for roundInProgress, useful for GameStatusDTO
    public boolean isRoundInProgress() {
        return roundInProgress;
    }
}