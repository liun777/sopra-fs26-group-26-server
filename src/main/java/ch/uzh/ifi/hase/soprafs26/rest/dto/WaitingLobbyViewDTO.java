package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class WaitingLobbyViewDTO {

    private Long lobbyId;
    private String sessionId;
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

    public List<WaitingLobbyPlayerRowDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<WaitingLobbyPlayerRowDTO> players) {
        this.players = players;
    }
}
