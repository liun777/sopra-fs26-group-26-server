package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class WaitingLobbyViewDTO extends LobbyConfigurableSettingsDTO {

    private Long lobbyId;
    private String sessionId;
    private Boolean viewerIsHost;
    private List<WaitingLobbyPlayerRowDTO> players = new ArrayList<>();
    private List<WaitingLobbyPlayerRowDTO> spectators = new ArrayList<>();

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

    public Boolean getViewerIsHost() {
        return viewerIsHost;
    }

    public void setViewerIsHost(Boolean viewerIsHost) {
        this.viewerIsHost = viewerIsHost;
    }

    public List<WaitingLobbyPlayerRowDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<WaitingLobbyPlayerRowDTO> players) {
        this.players = players;
    }

    public List<WaitingLobbyPlayerRowDTO> getSpectators() {
        return spectators;
    }

    public void setSpectators(List<WaitingLobbyPlayerRowDTO> spectators) {
        this.spectators = spectators;
    }
}
