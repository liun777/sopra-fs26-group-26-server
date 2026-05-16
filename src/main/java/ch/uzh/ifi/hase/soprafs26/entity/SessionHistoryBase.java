package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@MappedSuperclass
public abstract class SessionHistoryBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false)
    private Instant startTime = Instant.now();

    @Column(nullable = false)
    private boolean isEnded = false;

    @Column(nullable = false)
    private Long absentRoundPoints = 20L;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private List<Map<Long, Integer>> userScoresPerRound = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private Map<Long, Integer> totalScoreByUserId = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, unique = false)
    private Map<Long, Boolean> hundredReductionAppliedByUserId = new HashMap<>();

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

    public Long getAbsentRoundPoints() {
        return absentRoundPoints;
    }

    public void setAbsentRoundPoints(Long absentRoundPoints) {
        this.absentRoundPoints = absentRoundPoints;
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
        this.totalScoreByUserId = totalScoreByUserId != null ? totalScoreByUserId : new HashMap<>();
    }

    public Map<Long, Boolean> getHundredReductionAppliedByUserId() {
        return hundredReductionAppliedByUserId;
    }

    public void setHundredReductionAppliedByUserId(Map<Long, Boolean> hundredReductionAppliedByUserId) {
        this.hundredReductionAppliedByUserId = hundredReductionAppliedByUserId != null
                ? hundredReductionAppliedByUserId
                : new HashMap<>();
    }
}
