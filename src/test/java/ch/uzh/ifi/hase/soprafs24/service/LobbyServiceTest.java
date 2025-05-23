package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;

public class LobbyServiceTest {

    @InjectMocks
    private LobbyService lobbyService;

    @Mock
    private LobbyRepository lobbyRepository;
    private Lobby testLobby;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        testLobby = new Lobby();
        testLobby.setId(1L);
        testLobby.setTimeLimitSeconds(60L);
        Mockito.when(lobbyRepository.save(Mockito.any())).thenReturn(testLobby);
    }

    @Test
    public void testCreateLobby_success() {
        Lobby createdLobby = lobbyService.createLobby(1L, testLobby);
        assertNotNull(createdLobby);
        assertEquals(testLobby.getTimeLimitSeconds(), createdLobby.getTimeLimitSeconds());
        assertTrue(createdLobby.getPlayerReadyStatuses().containsKey(1L));
        assertFalse(createdLobby.getPlayerReadyStatuses().get(1L));
    }

    @Test
    public void addUserToLobby_success() {
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        Lobby updatedLobby = lobbyService.addUserToLobby(testLobby.getId(), 2L);
        System.out.println("Updated Lobby: " + updatedLobby);
        assertNotNull(updatedLobby);
        assertTrue(updatedLobby.getPlayerReadyStatuses().containsKey(2L));
        assertFalse(updatedLobby.getPlayerReadyStatuses().get(2L));
    }

    @Test
    public void testGetLobbyById_success() {
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        Lobby foundLobby = lobbyService.getLobbyById(testLobby.getId());
        assertNotNull(foundLobby);
        assertEquals(testLobby.getId(), foundLobby.getId());
    }

    @Test
    public void setUserReady_success() {
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        Lobby updatedLobby = lobbyService.addUserToLobby(testLobby.getId(), 2L);
        updatedLobby = lobbyService.setUserReady(testLobby.getId(), 2L);
        assertNotNull(updatedLobby);
        assertTrue(updatedLobby.getPlayerReadyStatuses().get(2L));
    }

}