package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SessionHistoryDTO {
    
    private Long id; // Changed from String to Long to match Session entity
    private String sessionId;
    private Instant startTime;
    private boolean isEnded;
    private List<Map<Long, Integer>> userScoresPerRound;
    private Map<Long, Integer> totalScoreByUserId;
    private Map<Long, Boolean> hundredReductionAppliedByUserId;

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
        this.userScoresPerRound = userScoresPerRound;
    }

    public Map<Long, Integer> getTotalScoreByUserId() {
        return totalScoreByUserId;
    }

    public void setTotalScoreByUserId(Map<Long, Integer> totalScoreByUserId) {
        this.totalScoreByUserId = totalScoreByUserId;
    }

    public Map<Long, Boolean> getHundredReductionAppliedByUserId() {
        return hundredReductionAppliedByUserId;
    }

    public void setHundredReductionAppliedByUserId(Map<Long, Boolean> hundredReductionAppliedByUserId) {
        this.hundredReductionAppliedByUserId = hundredReductionAppliedByUserId;
    }
}