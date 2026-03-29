package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class WaitingLobbyViewDTO {

    private Long lobbyId;
    private String sessionId;
    private Boolean isPublic;
    private List<WaitingLobbyPlayerRowDTO> players = new ArrayList<>();

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<WaitingLobbyPlayerRowDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<WaitingLobbyPlayerRowDTO> players) {
        this.players = players;
    }
}
