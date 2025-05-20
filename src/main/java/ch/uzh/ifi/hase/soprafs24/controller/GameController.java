package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;

import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;

import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/game")

public class GameController {

    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/{gameId}/start")
    public ResponseEntity<Game> startGame(@PathVariable Long gameId) {
        Game game = gameService.tryStartGame(gameId);
        // Returns the created Game with a CREATED status.
        return new ResponseEntity<>(game, HttpStatus.CREATED);
    }

    /**
     * Retrieves game details for the given game id.
     *
     * @param gameId The id of the game.
     * @return The GameManager representing the game state.
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameManager> getGame(@PathVariable Long gameId) {
        GameManager gameManager = gameService.getGame(gameId);
        return new ResponseEntity<>(gameManager, HttpStatus.OK);
    }

    /**
     * Checks if a game is active (exists in memory).
     *
     * @param gameId The id of the game.
     * @return A boolean indicating whether the game is currently active.
     */
    @GetMapping("/{gameId}/active")
    public ResponseEntity<Boolean> isGameActive(@PathVariable Long gameId) {
        boolean active = gameService.isGameActive(gameId);
        return new ResponseEntity<>(active, HttpStatus.OK);
    }

    @GetMapping("/{gameId}/round")
    public ResponseEntity<GameStatusDTO> getGameRoundStatus(@PathVariable Long gameId) {
        GameManager gameManager = gameService.getGame(gameId);
        if (gameManager == null) {
            return ResponseEntity.notFound().build();
        }

        long remainingTime = gameManager.getNextRoundStartTimeMillis() - System.currentTimeMillis();

        GameStatusDTO dto = new GameStatusDTO(
                gameManager.getCurrentRound(),
                gameManager.isActive(),
                remainingTime);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{gameId}/players/{userId}/state")
    public ResponseEntity<PlayerStateGetDTO> getPlayerState(
            @PathVariable Long gameId,
            @PathVariable Long userId) {
        GameManager game = gameService.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }

        PlayerState player = game.getPlayerState(userId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }

        PlayerStateGetDTO dto = new PlayerStateGetDTO();
        dto.setUserId(player.getUserId());
        dto.setCashBalance(player.getCashBalance());
        return ResponseEntity.ok(dto);
    }

    /**
     * We need to be able to advance when only all players submitted transaction in
     * addition to the timer running out
     * Pollable endpoint for clients to know whether:
     * - allSubmitted: every player has called submit for the current round
     * - roundEnded: either they’ve all submitted _or_ the server timer has
     * auto-advanced
     */
    @GetMapping("/{gameId}/status")
    public RoundStatusDTO getRoundStatus(
            @PathVariable Long gameId,
            @RequestParam(name = "lastRound", defaultValue = "0", required = false) Integer lastRound) {
        // 1) lookup your GameManager
        GameManager gm = gameService.getGame(gameId);
        if (gm == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }

        int current = gm.getCurrentRound();
        System.out.printf(
                "DEBUG: game %d status: currentRound=%d, lastRoundParam=%s%n",
                gameId, current, lastRound);

        // 2) have all players submitted for this round?
        boolean allSubmitted = gm.haveAllPlayersSubmittedForCurrentRound();

        // 3) has the round ended? Two cases:
        // a) everyone submitted
        // b) the server’s timer auto-advanced us past the client’s last‐seen round
        boolean roundEnded;
        if (lastRound != null) {
            roundEnded = current > lastRound || allSubmitted;
        } else {
            // fallback: treat any all-submitted as “ended”
            roundEnded = allSubmitted;
        }
        System.out.println("status for game " + gameId + " current=" + current + " lastRound=" + lastRound);
        return new RoundStatusDTO(allSubmitted, roundEnded, gm.getNextRoundStartTimeMillis());
    }

}
