package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.entity.GameMoveEvent;
import ch.uzh.ifi.hase.soprafs26.entity.GameMoveStep;


public class GameMoveEventDTO extends GameMoveEvent {

    @Override
    public GameMoveStepDTO getPrimary() {
        GameMoveStep step = super.getPrimary();
        return step == null ? null : (GameMoveStepDTO) step;
    }

    @Override
    public GameMoveStepDTO getSecondary() {
        GameMoveStep step = super.getSecondary();
        return step == null ? null : (GameMoveStepDTO) step;
    }
}
