package ch.uzh.ifi.hase.soprafs26.entity;

public class GameMoveEvent {
    private long sequence;
    private Long actorUserId;
    private GameMoveStep primary;
    private GameMoveStep secondary;

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

    public GameMoveStep getPrimary() {
        return primary;
    }

    public void setPrimary(GameMoveStep primary) {
        this.primary = primary;
    }

    public GameMoveStep getSecondary() {
        return secondary;
    }

    public void setSecondary(GameMoveStep secondary) {
        this.secondary = secondary;
    }
}

