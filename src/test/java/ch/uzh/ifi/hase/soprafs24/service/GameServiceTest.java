package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    public void testTryStartGame_success() {
        // Create a dummy lobby with all players ready
        Lobby lobby = new Lobby();
        Map<Long, Boolean> players = Map.of(1L, true, 2L, true);
        lobby.setPlayerReadyStatuses(players);
        lobby.setTimeLimitSeconds(600L);
        lobbyRepository.save(lobby);

        // Call the method
        Game game = gameService.tryStartGame(lobby.getId());

        // Assert game is created and stored
        assertNotNull(game.getId());
        assertEquals(lobby.getId(), game.getLobbyId());
        assertTrue(InMemoryGameRegistry.isGameActive(game.getId()));

    }
}
