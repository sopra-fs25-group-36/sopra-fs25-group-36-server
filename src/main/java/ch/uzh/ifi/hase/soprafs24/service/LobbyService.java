package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
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
    
    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    }
    
    @Scheduled(fixedRate = 1000)
    public void deactivateExpiredLobbies() {
        List<Lobby> activeLobbies = lobbyRepository.findByActiveTrue();
        LocalDateTime now = LocalDateTime.now();
        for (Lobby lobby : activeLobbies) {
            // If the current time is after the lobby's expiration time, mark it as inactive.
            if (lobby.getCreatedAt().plusSeconds(lobby.getTimeLimitSeconds()).isBefore(now)) {
                lobby.setActive(false);
                lobbyRepository.save(lobby);
            }
        }
    }

    public Lobby addUserToLobby(Long lobbyId, Long userId) {
        Lobby lobby = lobbyRepository.findById(lobbyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
        // Add the new user with a default ready status (false)
        lobby.getPlayerReadyStatuses().put(userId, false);
        return lobbyRepository.save(lobby);
    }
}

