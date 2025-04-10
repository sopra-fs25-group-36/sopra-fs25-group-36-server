package ch.uzh.ifi.hase.soprafs24.controller;

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

@RestController
@RequestMapping("/lobby")
public class GameController {

    private final GameService gameService;

    // ✅ Constructor-based injection (correct place!)
    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
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
