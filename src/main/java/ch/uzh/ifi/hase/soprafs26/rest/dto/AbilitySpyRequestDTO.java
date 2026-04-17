package ch.uzh.ifi.hase.soprafs26.rest.dto;

// request for POST /games/{gameId}/abilities/spy (9/10 ability)
public class AbilitySpyRequestDTO {

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
