package ch.uzh.ifi.hase.soprafs26.rest.dto; // sagt java dass diese Klasse zum DTO paket gehört

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

// dto = Data Transfer Object = Daten die durchs Netzwerk geschickt werden
public class UserPutDTO {

    private String password; // für wenn der User ein neues Passwort machen möchte
    // private weil es nur innerhalb dieser Klasse direkt zugänglich ist
    private UserStatus status; // damit das passwort auf offline gesetzt werden kann bei logout


    public String getPassword() { return password; }
    // Getter der Uns das Passwort im Forntend zurück gibt
    public UserStatus getStatus() {return status;}


    // aufgerufen vom DTOMapper um ps zu lesen
    public void setPassword(String password) { this.password = password; }
    // setter der das pw setzt
    // wird aufgerufen wenn frontend den Request schickt
    public void setStatus(UserStatus status) { this.status = status; }

}


