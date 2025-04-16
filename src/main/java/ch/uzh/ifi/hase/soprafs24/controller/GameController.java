package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;

import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerStateGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;

import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


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

    @GetMapping("/{lobbyId}/players/{userId}/state")
    public ResponseEntity<PlayerStateGetDTO> getPlayerState(
            @PathVariable Long lobbyId,
            @PathVariable Long userId
    ) {
        GameManager game = InMemoryGameRegistry.getGame(lobbyId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }

        PlayerState player = game.getPlayerState(userId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }

        Map<String, Double> prices = game.getCurrentStockPrices();
        Map<String, String> categories = gameService.getCategoryMap();

        List<StockHoldingDTO> stockDtos = gameService.toStockHoldings(player, prices, categories); // ✅ move this to GameService

        List<TransactionRequestDTO> transactions = player.getTransactionHistory().stream().map(tx -> {
            TransactionRequestDTO dto = new TransactionRequestDTO();
            dto.setStockId(tx.getStockId());
            dto.setQuantity(tx.getQuantity());
            dto.setStockPrice(tx.getPrice());
            dto.setType(tx.getType());
            return dto;
        }).toList();

        PlayerStateGetDTO dto = new PlayerStateGetDTO();
        dto.setUserId(player.getUserId());
        dto.setCashBalance(player.getCashBalance());
        dto.setStocks(stockDtos);
        dto.setTransactionHistory(transactions);

        return ResponseEntity.ok(dto);

    }
}
