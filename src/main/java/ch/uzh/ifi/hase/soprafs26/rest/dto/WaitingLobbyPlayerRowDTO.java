package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class WaitingLobbyPlayerRowDTO {

    private String username;

    private String joinStatus;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJoinStatus() {
        return joinStatus;
    }

    public void setJoinStatus(String joinStatus) {
        this.joinStatus = joinStatus;
    }
}
