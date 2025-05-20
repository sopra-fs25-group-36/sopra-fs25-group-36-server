package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Map;

/**
 * What the front-end receives.
 * `createdAt` is epoch milliseconds (easy for JavaScript Date).
 */
public class LobbyGetDTO {

  private Long id;
  private Map<Long, Boolean> playerReadyStatuses;
  private long createdAt; // epoch millis (UTC)
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

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
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
