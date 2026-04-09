package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class PeekSelectionDTO {
    private List<Integer> indices;

    public List<Integer> getIndices() {
        return indices;
    }

    public void setIndices(List<Integer> indices) {
        this.indices = indices;
    }
}

