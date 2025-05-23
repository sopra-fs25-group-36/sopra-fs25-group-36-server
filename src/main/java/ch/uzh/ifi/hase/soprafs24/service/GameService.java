package ch.uzh.ifi.hase.soprafs24.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;

@Service
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final StockService stockService;

    @Autowired
    public GameService(LobbyRepository lobbyRepository, GameRepository gameRepository, StockService stockService) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
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

        Game game = new Game();
        game.setId(lobbyId);
        game.setLobbyId(lobbyId);
        gameRepository.save(game);

        LinkedHashMap<LocalDate, Map<String, Double>> timeline = stockService.getStockTimelineFromDatabase();
        System.out.println("===== Stock Timeline for Game =====");
        int day = 1;
        for (Map.Entry<LocalDate, Map<String, Double>> entry : timeline.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Double> snapshot = entry.getValue();
            System.out.println("Day " + day++ + " (" + date + "):");
            snapshot.forEach((symbol, price) -> System.out.println("  " + symbol + ": " + price));
        }
        System.out.println("===================================");
        GameManager gameManager = new GameManager(game.getId(), timeline);
        lobby.getPlayerReadyStatuses().keySet().forEach(gameManager::registerPlayer);
        InMemoryGameRegistry.registerGame(game.getId(), gameManager);
        gameManager.startGame();
        lobby.setActive(false);
        lobbyRepository.save(lobby);
        return game;
    }

    public GameManager getGame(Long gameId) {
        GameManager game = InMemoryGameRegistry.getGame(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found for id: " + gameId);
        }
        return game;
    }

    public boolean isGameActive(Long gameId) {
        return InMemoryGameRegistry.isGameActive(gameId);
    }
}
