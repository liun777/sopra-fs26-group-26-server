package ch.uzh.ifi.hase.soprafs26.rest.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.Size;

// definiert was das frontend ans backend senset, also daten die der user eingegeben hat,
// enthält alle felder die für registrierung und Login benötigt werden

// unterschied zu GETDTO: POST = Frontend schickt ans Backend, GET = Backend schickt ans Frontend.
public class UserPostDTO {

	private String name;

	// restrict username length to 16 characters
	@Size(max = 16, message = "Username must be at most 16 characters long")
	private String username;

    private String password;

    private String bio;

    private LocalDate creationDate;

	public String getName() {
		return name;
	}
    // public damit dto mapper und service auf diese felder zugriff haben.


	public void setName(String name) {
		this.name = name;
	}
// void weil setter nichts zurück geben
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

    public String getPassword() {return password;}

    public void setPassword(String password) {this.password = password;}

    public String getBio() { return bio; }

    public void setBio(String bio) { this.bio = bio; }

    public LocalDate getCreationDate() {return creationDate; }

    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }
}
