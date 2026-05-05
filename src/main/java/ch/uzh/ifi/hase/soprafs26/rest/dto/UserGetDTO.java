package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import java.time.LocalDate;

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


}

