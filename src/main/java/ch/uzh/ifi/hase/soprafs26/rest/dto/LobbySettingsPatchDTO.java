package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LobbySettingsPatchDTO {

    private Boolean isPublic;
    private Long afkTimeoutSeconds;
    private Long initialPeekSeconds;
    private Long turnSeconds;
    private Long abilityRevealSeconds;
    private Long rematchDecisionSeconds;
    private Long websocketGraceSeconds;

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
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

    public Long getWebsocketGraceSeconds() {
        return websocketGraceSeconds;
    }

    public void setWebsocketGraceSeconds(Long websocketGraceSeconds) {
        this.websocketGraceSeconds = websocketGraceSeconds;
    }
}
