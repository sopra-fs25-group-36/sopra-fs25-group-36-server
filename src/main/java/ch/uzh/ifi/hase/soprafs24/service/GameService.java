package ch.uzh.ifi.hase.soprafs24.service;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.game.PlayerState;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.service.StockService;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDate;
import java.util.*;

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
        game.setId(lobbyId);
        game.setLobbyId(lobbyId);
        gameRepository.save(game);

        // Generate stock timeline
        LinkedHashMap<LocalDate, Map<String, Double>> timeline = stockService.getStockTimelineFromDatabase();
        //check if it is right by printing to console
        System.out.println("===== Stock Timeline for Game =====");
        int day = 1;
        for (Map.Entry<LocalDate, Map<String, Double>> entry : timeline.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Double> snapshot = entry.getValue();
            System.out.println("Day " + day++ + " (" + date + "):");
            snapshot.forEach((symbol, price) -> System.out.println("  " + symbol + ": " + price));
        }
        System.out.println("===================================");

//        // Create and register GameManager
////        List<Map<String, Double>> timelineList = new ArrayList<>();
//
//        // debugging data structure
//        for (Map<String, Double> snapshot : timeline.values()) {
//            try {
//                Map<String, Double> mutable = new HashMap<>(snapshot);
//                timelineList.add(mutable);
//                System.out.println("debug passed");
//
//            } catch (UnsupportedOperationException e) {
//                System.out.println("⚠ Snapshot was unmodifiable: " + snapshot.getClass().getName());
//                throw e;
//            }
//        }
//
//        for (Map<String, Double> snapshot : timeline.values()) {
//            timelineList.add(new HashMap<>(snapshot)); // deep copy each day's map
//        }

        GameManager gameManager = new GameManager(game.getId(), timeline); // timeline is LinkedHashMap
        lobby.getPlayerReadyStatuses().keySet().forEach(gameManager::registerPlayer);

        InMemoryGameRegistry.registerGame(game.getId(), gameManager);

        // Kick off the first round’s timeout
                gameManager.startGame();

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

    // public Map<String, String> getCategoryMap() {
    //     Map<String, String> map = new HashMap<>();

    //     map.put("AAPL", "Tech");
    //     map.put("MSFT", "Tech");
    //     map.put("XOM", "Energy");
    //     map.put("CVX", "Energy");
    //     map.put("JPM", "Finance");
    //     map.put("GS", "Finance");
    //     map.put("JNJ", "Healthcare");
    //     map.put("PFE", "Healthcare");
    //     map.put("PG", "Consumer");
    //     map.put("KO", "Consumer");

    //     return map;
    // }
    // public List<StockHoldingDTO> toStockHoldings(PlayerState player,
    //                                              Map<String, Double> prices) {
    //     List<StockHoldingDTO> holdings = new ArrayList<>();

    //     for (Map.Entry<String, Integer> entry : player.getPlayerStocks().entrySet()) {
    //         String symbol = entry.getKey();
    //         int quantity = entry.getValue();
    //         double price = prices.getOrDefault(symbol, 0.0);

    //         StockHoldingDTO dto = new StockHoldingDTO();
    //         dto.setSymbol(symbol);
    //         dto.setQuantity(quantity);
    //         dto.setCurrentPrice(price);

    //         holdings.add(dto);
    //     }

    //     return holdings;
    }




