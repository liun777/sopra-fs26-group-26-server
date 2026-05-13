package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class FriendRequestIncomingDTO {
    private Long requesterUserId;
    private String requesterUsername;

    public Long getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(Long requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }
}
