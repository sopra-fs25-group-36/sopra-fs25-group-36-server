package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    @PostMapping("/{gameId}/submit")
    public ResponseEntity<String> submitTransaction(
            @PathVariable Long gameId,
            @RequestParam Long userId,
            @RequestBody List<TransactionRequestDTO> transactionsRequest) {

        GameManager gameManager = InMemoryGameRegistry.getGame(gameId);
        if (gameManager == null) {
            return ResponseEntity.badRequest().body("Game not found.");
        }

        gameManager.submitTransactions(userId, transactionsRequest);
        return ResponseEntity.ok("Transactions submitted.");
    }
}
