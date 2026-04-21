package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GameMoveEventDTO {
    private long sequence;
    private Long actorUserId;
    private GameMoveStepDTO primary;
    private GameMoveStepDTO secondary;

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public GameMoveStepDTO getPrimary() {
        return primary;
    }

    public void setPrimary(GameMoveStepDTO primary) {
        this.primary = primary;
    }

    public GameMoveStepDTO getSecondary() {
        return secondary;
    }

    public void setSecondary(GameMoveStepDTO secondary) {
        this.secondary = secondary;
    }
}

