package ch.uzh.ifi.hase.soprafs24.service;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.service.StockService;
import org.springframework.web.server.ResponseStatusException;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final StockService stockService;

    @Autowired
    public GameService(LobbyRepository lobbyRepository, GameRepository gameRepository, StockService stockService) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
//        this.stockService = stockService;
        this.stockService = stockService;
    }

    public Game tryStartGame(Long lobbyId) {
        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        if (!lobby.isActive()) {
            throw new IllegalStateException("Lobby is no longer active");
        }

        boolean allReady = lobby.getPlayerReadyStatuses().values().stream().allMatch(Boolean::booleanValue);
        if (!allReady) {
            throw new IllegalStateException("Not all players are ready");
        }

        // Create Game entity
        Game game = new Game();
        game.setLobbyId(lobbyId);
        gameRepository.save(game);

        // Generate stock timeline
        List<Map<String, Double>> stockTimeline = stockService.getStockTimelineFromDatabase();
        //check if it is right by printing to console
        System.out.println("===== Stock Timeline for Game =====");
        int day = 1;
        for (Map<String, Double> snapshot : stockTimeline) {
            System.out.println("Day " + day++ + ":");
            snapshot.forEach((symbol, price) -> System.out.println("  " + symbol + ": " + price));
        }
        System.out.println("===================================");

        // Create and register GameManager
        GameManager gameManager = new GameManager(game.getId(), stockTimeline);
        lobby.getPlayerReadyStatuses().keySet().forEach(gameManager::registerPlayer);

        InMemoryGameRegistry.registerGame(game.getId(), gameManager);

        // Start round progression timer
        gameManager.scheduleRounds();

        // Mark lobby as inactive
        lobby.setActive(false);
        lobbyRepository.save(lobby);

        return game;
    }

    //Retrieve the GameManager for a specific gameId.
    public GameManager getGame(Long gameId) {
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found for id: " + gameId);
        }
        return game;
    }

    //Check whether a game is currently active in memory.
    public boolean isGameActive(Long gameId) {
        return InMemoryGameRegistry.isGameActive(gameId);
    }
}


