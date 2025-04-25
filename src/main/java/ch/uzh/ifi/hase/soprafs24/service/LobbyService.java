package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class LobbyService {

    private final LobbyRepository lobbyRepository;

    public LobbyService(LobbyRepository lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
    }

    /* ---------- create ---------- */

    public Lobby createLobby(Long userId, Lobby lobbyInput) {
        lobbyInput.setTimeLimitSeconds(300L);             // 5 min default
        lobbyInput.getPlayerReadyStatuses().put(userId, false);
        return lobbyRepository.save(lobbyInput);
    }

    /* ---------- read ---------- */

    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    }

    /* ---------- scheduled: deactivate old lobbies ---------- */

    @Scheduled(fixedRate = 1_000)
    public void deactivateExpiredLobbies() {
        Instant now = Instant.now();
        List<Lobby> activeLobbies = lobbyRepository.findByActiveTrue();

        for (Lobby lobby : activeLobbies) {
            Instant expiresAt = lobby.getCreatedAt()
                                      .plusSeconds(lobby.getTimeLimitSeconds());
            if (expiresAt.isBefore(now)) {
                lobby.setActive(false);
                lobbyRepository.save(lobby);
            }
        }
    }

    /* ---------- membership + ready ---------- */

    public Lobby addUserToLobby(Long lobbyId, Long userId) {
        Lobby lobby = getLobbyById(lobbyId);
        lobby.getPlayerReadyStatuses().put(userId, false);
        return lobbyRepository.save(lobby);
    }

    public Lobby setUserReady(Long lobbyId, Long userId) {
        Lobby lobby = getLobbyById(lobbyId);

        if (!lobby.getPlayerReadyStatuses().containsKey(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "User is not part of the lobby");
        }

        lobby.getPlayerReadyStatuses().put(userId, true);
        return lobbyRepository.save(lobby);
    }
}
