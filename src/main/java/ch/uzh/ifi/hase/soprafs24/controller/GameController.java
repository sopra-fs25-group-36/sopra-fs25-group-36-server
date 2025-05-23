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
        return new ResponseEntity<>(game, HttpStatus.CREATED);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameManager> getGame(@PathVariable Long gameId) {
        GameManager gameManager = gameService.getGame(gameId);
        return new ResponseEntity<>(gameManager, HttpStatus.OK);
    }

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

    @GetMapping("/{gameId}/status")
    public RoundStatusDTO getRoundStatus(
            @PathVariable Long gameId,
            @RequestParam(name = "lastRound", defaultValue = "0", required = false) Integer lastRound) {
        GameManager gm = gameService.getGame(gameId);
        if (gm == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }

        int current = gm.getCurrentRound();
        System.out.printf(
                "DEBUG: game %d status: currentRound=%d, lastRoundParam=%s%n",
                gameId, current, lastRound);

        boolean allSubmitted = gm.haveAllPlayersSubmittedForCurrentRound();

        boolean roundEnded;
        if (lastRound != null) {
            roundEnded = current > lastRound || allSubmitted;
        } else {
            roundEnded = allSubmitted;
        }
        System.out.println("status for game " + gameId + " current=" + current + " lastRound=" + lastRound);
        return new RoundStatusDTO(allSubmitted, roundEnded, gm.getNextRoundStartTimeMillis());
    }

}
