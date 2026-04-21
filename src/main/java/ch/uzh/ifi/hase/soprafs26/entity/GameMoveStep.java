package ch.uzh.ifi.hase.soprafs26.entity;

public class GameMoveStep {
    private String sourceZone;
    private Long sourceUserId;
    private Integer sourceCardIndex;
    private String targetZone;
    private Long targetUserId;
    private Integer targetCardIndex;
    private boolean hidden;
    private Integer value;

    public String getSourceZone() {
        return sourceZone;
    }

    public void setSourceZone(String sourceZone) {
        this.sourceZone = sourceZone;
    }

    public Long getSourceUserId() {
        return sourceUserId;
    }

    public void setSourceUserId(Long sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    public Integer getSourceCardIndex() {
        return sourceCardIndex;
    }

    public void setSourceCardIndex(Integer sourceCardIndex) {
        this.sourceCardIndex = sourceCardIndex;
    }

    public String getTargetZone() {
        return targetZone;
    }

    public void setTargetZone(String targetZone) {
        this.targetZone = targetZone;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Integer getTargetCardIndex() {
        return targetCardIndex;
    }

    public void setTargetCardIndex(Integer targetCardIndex) {
        this.targetCardIndex = targetCardIndex;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}

