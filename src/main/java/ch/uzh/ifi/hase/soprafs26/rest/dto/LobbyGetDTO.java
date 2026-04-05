package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

// this can be used to return lobbies to the frontend
public class LobbyGetDTO {
    
    private Long id;
    private String sessionId;
    private Long sessionHostUserId;
    private List<Long> playerIds;
    private Boolean isPublic;
    private Integer currentRound;
    private String status;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionHostUserId(Long sessionHostUserId) {
        this.sessionHostUserId = sessionHostUserId;
    }

    public Long getSessionHostUserId() {
        return sessionHostUserId;
    }

    public void setPlayerIds(List<Long> playerIds) {
        this.playerIds = playerIds;
    }

    public List<Long> getPlayerIds() {
        return playerIds;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}