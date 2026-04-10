package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class PeekSelectionDTO {

    // initial or special, see PeekType class
    private String peekType;

    // initial type: empty or equal to authenticated user, otherwise 403. 
    // special type: future special ability work
    private Long handUserId;
    private List<Integer> indices;

    public String getPeekType() {
        return peekType;
    }

    public void setPeekType(String peekType) {
        this.peekType = peekType;
    }

    public Long getHandUserId() {
        return handUserId;
    }

    public void setHandUserId(Long handUserId) {
        this.handUserId = handUserId;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public void setIndices(List<Integer> indices) {
        this.indices = indices;
    }
}
