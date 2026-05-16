package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LobbyConfigurableSettingsDTO extends LobbyTimingSettingsDTO {

    private Boolean isPublic;
    private Long absentRoundPoints;

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Long getAbsentRoundPoints() {
        return absentRoundPoints;
    }

    public void setAbsentRoundPoints(Long absentRoundPoints) {
        this.absentRoundPoints = absentRoundPoints;
    }
}
