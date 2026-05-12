package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class WaitingLobbyPlayerRowDTO {

    private Long userId;

    private String username;

    private String joinStatus;

    private String profileCharacterId;

    private String characterColorId;

    private Boolean ready;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public String getProfileCharacterId() {
        return profileCharacterId;
    }

    public void setProfileCharacterId(String profileCharacterId) {
        this.profileCharacterId = profileCharacterId;
    }

    public String getCharacterColorId() {
        return characterColorId;
    }

    public void setCharacterColorId(String characterColorId) {
        this.characterColorId = characterColorId;
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }
}
