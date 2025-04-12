package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.LeaderBoardEntry;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderBoardEntryGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Starts a new game for the given lobby.
     *
     * @param lobbyId The id of the lobby from which to start the game.
     * @return The created Game entity.
     */
    @PostMapping("/{gameId}/start")
    public ResponseEntity<Game> startGame(@RequestParam Long gameId) {
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
}
