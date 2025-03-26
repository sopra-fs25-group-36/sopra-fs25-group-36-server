package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LobbyService {

    private final LobbyRepository lobbyRepository;

    public LobbyService(LobbyRepository lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
    }

    /**
     * Creates a new lobby for the specified creator.
     *
     * @param userId The ID of the user creating the lobby.
     * @param lobbyInput A Lobby object built from the DTO (LobbyPostDTO), initially empty.
     * @return The persisted Lobby entity.
     */
    public Lobby createLobby(Long userId, Lobby lobbyInput) {
        // Set a predetermined time limit for the lobby (e.g., 300 seconds)
        lobbyInput.setTimeLimitSeconds(300L);

        // Add the creator to the lobby's playerReadyStatuses with a default ready status (false)
        lobbyInput.getPlayerReadyStatuses().put(userId, false);

        // The @PrePersist method in the Lobby entity will automatically set createdAt and active.
        return lobbyRepository.save(lobbyInput);
    }
    
    // Additional methods (e.g., joinLobby, updatePlayerStatus) can be added here as needed.
}

