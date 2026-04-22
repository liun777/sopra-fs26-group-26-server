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

    // Users kicked by host from this waiting lobby.
    // They may rejoin only through an explicit host invite flow.
    @ElementCollection
    private List<Long> kickedUserIds = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isPublic = true;

    @Column(nullable = false)
    private Integer currentRound = 0;

    @Column(nullable = false)
    private String status = "WAITING"; // WAITING or PLAYING // prob later SPECTATING

    // Per-lobby AFK timeout used while the game is active.
    @Column(nullable = false)
    private Long afkTimeoutSeconds = 300L;

    // Per-lobby initial peek timer.
    @Column(nullable = false)
    private Long initialPeekSeconds = 10L;

    // Per-lobby turn timer.
    @Column(nullable = false)
    private Long turnSeconds = 30L;

    // Per-lobby reveal duration for peek/spy before auto-end.
    @Column(nullable = false)
    private Long abilityRevealSeconds = 5L;

    // Per-lobby rematch decision window.
    @Column(nullable = false)
    private Long rematchDecisionSeconds = 60L;

    // Per-lobby websocket disconnect grace period before timeout handling.
    @Column(nullable = false)
    private Long websocketGraceSeconds = 300L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getSessionHostUserId() { return sessionHostUserId; }
    public void setSessionHostUserId(Long sessionHostUserId) { this.sessionHostUserId = sessionHostUserId; }

    public List<Long> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<Long> playerIds) { this.playerIds = playerIds; }

    public List<Long> getKickedUserIds() { return kickedUserIds; }
    public void setKickedUserIds(List<Long> kickedUserIds) { this.kickedUserIds = kickedUserIds; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public Integer getCurrentRound() { return currentRound; }
    public void setCurrentRound(Integer currentRound) { this.currentRound = currentRound; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getAfkTimeoutSeconds() { return afkTimeoutSeconds; }
    public void setAfkTimeoutSeconds(Long afkTimeoutSeconds) { this.afkTimeoutSeconds = afkTimeoutSeconds; }

    public Long getInitialPeekSeconds() { return initialPeekSeconds; }
    public void setInitialPeekSeconds(Long initialPeekSeconds) { this.initialPeekSeconds = initialPeekSeconds; }

    public Long getTurnSeconds() { return turnSeconds; }
    public void setTurnSeconds(Long turnSeconds) { this.turnSeconds = turnSeconds; }

    public Long getAbilityRevealSeconds() { return abilityRevealSeconds; }
    public void setAbilityRevealSeconds(Long abilityRevealSeconds) { this.abilityRevealSeconds = abilityRevealSeconds; }

    public Long getRematchDecisionSeconds() { return rematchDecisionSeconds; }
    public void setRematchDecisionSeconds(Long rematchDecisionSeconds) { this.rematchDecisionSeconds = rematchDecisionSeconds; }

    public Long getWebsocketGraceSeconds() { return websocketGraceSeconds; }
    public void setWebsocketGraceSeconds(Long websocketGraceSeconds) { this.websocketGraceSeconds = websocketGraceSeconds; }
}
