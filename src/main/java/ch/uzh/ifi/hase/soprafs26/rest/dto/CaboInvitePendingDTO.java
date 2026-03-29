package ch.uzh.ifi.hase.soprafs26.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CaboInvitePendingDTO {

    private Long id;
    private Long fromUserId;
    private String fromUsername;
    private String fromName;
    private String sessionId;
    private Long sessionHostUserId;
    private String inviteCreationDate;

    @JsonProperty("inviteId")
    public String getInviteId() {
        return id == null ? null : String.valueOf(id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getSessionHostUserId() {
        return sessionHostUserId;
    }

    public void setSessionHostUserId(Long sessionHostUserId) {
        this.sessionHostUserId = sessionHostUserId;
    }

    public String getInviteCreationDate() {
        return inviteCreationDate;
    }

    public void setInviteCreationDate(String inviteCreationDate) {
        this.inviteCreationDate = inviteCreationDate;
    }
}
