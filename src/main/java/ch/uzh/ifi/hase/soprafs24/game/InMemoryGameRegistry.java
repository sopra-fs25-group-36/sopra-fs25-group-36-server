package ch.uzh.ifi.hase.soprafs24.game;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryGameRegistry {

    private static final Map<Long, GameManager> activeGames = new ConcurrentHashMap<>();

    public static void registerGame(Long gameId, GameManager gameManager) {
        activeGames.put(gameId, gameManager);
    }

    public static GameManager getGame(Long gameId) {
        return activeGames.get(gameId);
    }

    public static void remove(Long gameId) {
        activeGames.remove(gameId);
    }

    public static boolean isGameActive(Long gameId) {
        return activeGames.containsKey(gameId);
    }
    public static void clear() {
        activeGames.clear();
    }

}
