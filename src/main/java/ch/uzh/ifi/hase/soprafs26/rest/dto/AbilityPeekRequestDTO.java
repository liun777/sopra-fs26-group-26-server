package ch.uzh.ifi.hase.soprafs26.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

// request for POST /games/{gameId}/abilities/peek (7/8 ability)
public class AbilityPeekRequestDTO {

    @JsonAlias({"userid", "userId"})
    private Long userId;

    private Integer targetCardIndex;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getTargetCardIndex() {
        return targetCardIndex;
    }

    public void setTargetCardIndex(Integer targetCardIndex) {
        this.targetCardIndex = targetCardIndex;
    }
}
