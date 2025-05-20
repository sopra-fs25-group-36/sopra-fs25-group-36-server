package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal Lobby representation.
 */
@Entity
@Table(name = "LOBBY")
public class Lobby implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** player-id ➜ ready? */
    @ElementCollection
    @CollectionTable(name = "lobby_player_status", joinColumns = @JoinColumn(name = "lobby_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "ready")
    private Map<Long, Boolean> playerReadyStatuses = new HashMap<>();

    /** UTC moment when lobby was created */
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;

    @Column(nullable = false)
    private boolean active;

    /** length of lobby in seconds */
    @Column(nullable = false)
    private Long timeLimitSeconds;

    /* ---------- life-cycle ---------- */

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now(); // absolute, unambiguous
        this.active = true;
    }

    /* ---------- getters / setters ---------- */

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
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
