package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lobbies")
public class Lobby implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId; // unique code players use to find the lobby

    @Column(nullable = false)
    private Long sessionHostUserId; // userId of the creator

    @ElementCollection
    private List<Long> playerIds = new ArrayList<>(); // userIds of players in lobby

    @Column(nullable = false)
    private Boolean isPublic = true;

    @Column(nullable = false)
    private Integer currentRound = 0;

    @Column(nullable = false)
    private String status = "WAITING"; // WAITING or IN_GAME

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getSessionHostUserId() { return sessionHostUserId; }
    public void setSessionHostUserId(Long sessionHostUserId) { this.sessionHostUserId = sessionHostUserId; }

    public List<Long> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<Long> playerIds) { this.playerIds = playerIds; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public Integer getCurrentRound() { return currentRound; }
    public void setCurrentRound(Integer currentRound) { this.currentRound = currentRound; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}