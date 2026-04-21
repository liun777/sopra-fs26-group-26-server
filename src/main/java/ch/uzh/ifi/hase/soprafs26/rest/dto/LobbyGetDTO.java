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
    private Long afkTimeoutSeconds;
    private Long initialPeekSeconds;
    private Long turnSeconds;
    private Long abilityRevealSeconds;
    private Long rematchDecisionSeconds;

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

    public Long getAfkTimeoutSeconds() {
        return afkTimeoutSeconds;
    }

    public void setAfkTimeoutSeconds(Long afkTimeoutSeconds) {
        this.afkTimeoutSeconds = afkTimeoutSeconds;
    }

    public Long getInitialPeekSeconds() {
        return initialPeekSeconds;
    }

    public void setInitialPeekSeconds(Long initialPeekSeconds) {
        this.initialPeekSeconds = initialPeekSeconds;
    }

    public Long getTurnSeconds() {
        return turnSeconds;
    }

    public void setTurnSeconds(Long turnSeconds) {
        this.turnSeconds = turnSeconds;
    }

    public Long getAbilityRevealSeconds() {
        return abilityRevealSeconds;
    }

    public void setAbilityRevealSeconds(Long abilityRevealSeconds) {
        this.abilityRevealSeconds = abilityRevealSeconds;
    }

    public Long getRematchDecisionSeconds() {
        return rematchDecisionSeconds;
    }

    public void setRematchDecisionSeconds(Long rematchDecisionSeconds) {
        this.rematchDecisionSeconds = rematchDecisionSeconds;
    }
}
