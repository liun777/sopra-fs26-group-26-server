package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false)
    private Instant startTime = Instant.now();

    @Column(nullable = false)
    private boolean isEnded = false;

    // List entry = one round; map key=userId, value=score for that round.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private List<Map<Long, Integer>> userScoresPerRound = new ArrayList<>();

    // session total score per player (updated at round end)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private Map<Long, Integer> totalScoreByUserId = new java.util.HashMap<>();

    // tracks whether the "totalScore == 100 -> 50" correction was already applied for a player
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private Map<Long, Boolean> hundredReductionAppliedByUserId = new java.util.HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public boolean isEnded() {
        return isEnded;
    }

    public void setEnded(boolean ended) {
        isEnded = ended;
    }

    public List<Map<Long, Integer>> getUserScoresPerRound() {
        return userScoresPerRound;
    }

    public void setUserScoresPerRound(List<Map<Long, Integer>> userScoresPerRound) {
        this.userScoresPerRound = userScoresPerRound != null ? userScoresPerRound : new ArrayList<>();
    }

    public Map<Long, Integer> getTotalScoreByUserId() {
        return totalScoreByUserId;
    }

    public void setTotalScoreByUserId(Map<Long, Integer> totalScoreByUserId) {
        this.totalScoreByUserId = totalScoreByUserId != null ? totalScoreByUserId : new java.util.HashMap<>();
    }

    public Map<Long, Boolean> getHundredReductionAppliedByUserId() {
        return hundredReductionAppliedByUserId;
    }

    public void setHundredReductionAppliedByUserId(Map<Long, Boolean> hundredReductionAppliedByUserId) {
        this.hundredReductionAppliedByUserId = hundredReductionAppliedByUserId != null
                ? hundredReductionAppliedByUserId
                : new java.util.HashMap<>();
    }
}
