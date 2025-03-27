package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal Lobby Representation
 * This class defines how the lobby is stored in the database.
 * A lobby includes:
 * - A unique id.
 * - A mapping of players (by user id) to their ready status.
 * - A creation timestamp (timer start).
 * - A flag indicating if the lobby is still active.
 * - A time limit for the lobby.
 */
@Entity
@Table(name = "LOBBY")
public class Lobby implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mapping of player user IDs to their ready status.
     * This uses an ElementCollection so that the mapping is stored in a separate table.
     */
    @ElementCollection
    @CollectionTable(name = "lobby_player_status", joinColumns = @JoinColumn(name = "lobby_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "ready")
    private Map<Long, Boolean> playerReadyStatuses = new HashMap<>();

    /**
     * The timestamp when the lobby was created.
     * This can be used as the start of the lobby timer.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Indicates whether the lobby is still active/alive.
     */
    @Column(nullable = false)
    private boolean active;

    /**
     * The time limit (in seconds) for the lobby.
     */
    @Column(nullable = false)
    private Long timeLimitSeconds;

    // Default constructor
    public Lobby() {
        // Fields will be set in the @PrePersist method.
    }

    /**
     * Automatically set creation time and active status when persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<Long, Boolean> getPlayerReadyStatuses() {
        return playerReadyStatuses;
    }

    public void setPlayerReadyStatuses(Map<Long, Boolean> playerReadyStatuses) {
        this.playerReadyStatuses = playerReadyStatuses;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Long timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }
}

