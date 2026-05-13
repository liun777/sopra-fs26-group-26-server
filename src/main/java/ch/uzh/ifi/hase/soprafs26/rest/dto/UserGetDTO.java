package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import java.time.LocalDate;
import java.util.List;

// hier wird definiert was das backend ans frontend senden, wenn ein user abgerufen wird
// also alle Felder ausser das PW weil wir ja safety wollen
// DTO = Datatransfer object - Packet was durch das netzwerk gesendet wird.
public class UserGetDTO {

	private Long id; // zuerst Felder deklarieren in der Klasse
	private String name;
	private String username;

	private String token;
	private UserStatus status;
    private String bio;
    private LocalDate creationDate;
    private Integer gamesWon;
    private Integer roundsWon;
    private Integer averageScorePerSession;
    private Integer averageScorePerRound;
    private Integer overallRank;
    private Boolean isPublicLog;
    private String profileCharacterId;
    private List<String> preferredColorPriority;
    private String menuBackgroundId;
    private String gameBackgroundId;
    private String primaryColorId;
    private String appearanceMode;
    private Boolean tutorialsEnabled;
    private Integer musicVolume;
    private Integer soundEffectsVolume;
    private List<String> musicBlacklist;

	public Long getId() {
		return id;
	} // felder sind private also von aussen nicht zugänglich, aber get und set
    // müssssen public sein für den Controller und mapper, sonst erhaltet das frontend nichts

	public void setId(Long id) {
		this.id = id;
	}
    // void heisst es kommt nichts von der FUnktion zurück, setter setzt einfach nur einen
    // Wert der setter gibt nichts zurück, ein getter hingegen schon obvi..

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

    public String getBio() { return bio; }

    public void setBio(String bio) { this.bio = bio; }

    public LocalDate getCreationDate() { return creationDate; }

    public void setCreationDate(LocalDate creationDate) {this.creationDate = creationDate; }


    public Integer getGamesWon() { return gamesWon; }

    public void setGamesWon(Integer gamesWon) { this.gamesWon = gamesWon; }

    public Integer getRoundsWon() { return roundsWon; }

    public void setRoundsWon(Integer roundsWon) { this.roundsWon = roundsWon; }

    public Integer getAverageScorePerSession() { return averageScorePerSession; }

    public void setAverageScorePerSession(Integer averageScorePerSession) { this.averageScorePerSession = averageScorePerSession; }

    public Integer getAverageScorePerRound() { return averageScorePerRound; }

    public void setAverageScorePerRound(Integer averageScorePerRound) { this.averageScorePerRound = averageScorePerRound; }
	

    public Integer getOverallRank() { return overallRank; }

    public void setOverallRank(Integer overallRank) { this.overallRank = overallRank; }

    public Boolean getIsPublicLog() { return isPublicLog; }

    public void setIsPublicLog(Boolean isPublicLog) { this.isPublicLog = isPublicLog; }

    public String getProfileCharacterId() { return profileCharacterId; }

    public void setProfileCharacterId(String profileCharacterId) { this.profileCharacterId = profileCharacterId; }

    public List<String> getPreferredColorPriority() { return preferredColorPriority; }

    public void setPreferredColorPriority(List<String> preferredColorPriority) { this.preferredColorPriority = preferredColorPriority; }

    public String getMenuBackgroundId() { return menuBackgroundId; }

    public void setMenuBackgroundId(String menuBackgroundId) { this.menuBackgroundId = menuBackgroundId; }

    public String getGameBackgroundId() { return gameBackgroundId; }

    public void setGameBackgroundId(String gameBackgroundId) { this.gameBackgroundId = gameBackgroundId; }

    public String getPrimaryColorId() { return primaryColorId; }

    public void setPrimaryColorId(String primaryColorId) { this.primaryColorId = primaryColorId; }

    public String getAppearanceMode() { return appearanceMode; }

    public void setAppearanceMode(String appearanceMode) { this.appearanceMode = appearanceMode; }

    public Boolean getTutorialsEnabled() { return tutorialsEnabled; }

    public void setTutorialsEnabled(Boolean tutorialsEnabled) { this.tutorialsEnabled = tutorialsEnabled; }

    public Integer getMusicVolume() { return musicVolume; }

    public void setMusicVolume(Integer musicVolume) { this.musicVolume = musicVolume; }

    public Integer getSoundEffectsVolume() { return soundEffectsVolume; }

    public void setSoundEffectsVolume(Integer soundEffectsVolume) { this.soundEffectsVolume = soundEffectsVolume; }

    public List<String> getMusicBlacklist() { return musicBlacklist; }

    public void setMusicBlacklist(List<String> musicBlacklist) { this.musicBlacklist = musicBlacklist; }


}
