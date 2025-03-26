package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class LobbyGetDTO {

  private Long id;
  private Map<Long, Boolean> playerReadyStatuses;
  private LocalDateTime createdAt;
  private boolean active;
  private Long timeLimitSeconds;

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
