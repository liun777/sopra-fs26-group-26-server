package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */

// basically schnittstelle zwischen front und backend,
// empfängt alle http requests vom frontend
@RestController // sagt Spring dass diese Klasse REST Endpoints hat
public class UserController {

	private final UserService userService; // zugriff auf den userService für Logik

	UserController(UserService userService) {
		this.userService = userService;
	} // UserService wird automatisch von Spring injiziert

    // GET /users - alle User laden (zb auf Userliste) von frontend aufgerufen
	@GetMapping("/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getAllUsers() { // holt alle user aus der datenbank via service
		// fetch all users in the internal representation
		List<User> users = userService.getUsers(); // users ist nun liste mit allen usern aufgelistet
		List<UserGetDTO> userGetDTOs = new ArrayList<>(); // erstellen neuer leerer liste für die dtos (version der user die ans frontend geschickt werden ohne passwort)

		// convert each user to the API representation
		for (User user : users) { // geht durch alle user und wandelt in dto um und addet sie zur liste
			userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user)); // siehe dto skripte im rest ordner
		}
		return userGetDTOs; // fertige liste wird dann ans frontend gesendet
	}
// POST /users - frontend ruft das auf wenn ein neuer User sich registriert
	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
		// convert API user to internal representation
        // @RequestBody holt die userdaten (username, password, bio) aus dem request body des frontends
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO); // DTO zu Entity umwandeln

		// create user
		User createdUser = userService.createUser(userInput); // user in datenbank speichern via service
		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser); // fertigen user ans frontend schicken
	}
    // GET/users - für das Abrufen eines einzelnen Profils des frontends
    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getUserById(@PathVariable Long userId) {User user = userService.getUserById(userId);
        // @PathVariable holt die userid aus url und return schikt user ans Fromtend
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user) ;
    }

    // PUT /users/{userId}- für Ändern des Passworts
    @PutMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // user id aus der URL wird als Variable geholt und die Daten im Body des Requests ebenfalls
    public void updateUser(@PathVariable Long userId, @RequestBody UserPutDTO userPutDTO) {
        User userInput = DTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO);
        userService.updateUser(userId, userInput);
    }
        // @PathVariable holt userId aus URL, @RequestBody holt neues passwort aus request body


    // POST /login - wenn user sich einloggen will
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
        // @RequestBody holt username und passwort aus dem request body
        User user = userService.loginUser(userPostDTO.getUsername(), userPostDTO.getPassword()); // credentials werden geprüft
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user); // eingeloggten user ans frontend senden
    }

}
