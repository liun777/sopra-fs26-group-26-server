package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.Instant;

// #111 session log entry returned by GET /sessions/{id}/log
public class MoveLogEntryDTO {

    private Long userId;
    private String username;
    private String actionType;
    private Instant timestamp;
    private String details;
    private Boolean ownMove;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Boolean getOwnMove() { return ownMove; }
    public void setOwnMove(Boolean ownMove) { this.ownMove = ownMove; }
}
