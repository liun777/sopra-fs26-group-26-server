package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */

// "Übersetzer" zwischen Entity und DTO -definiert welches feld im Entity
// welchem im dto entspricht
    // sagt basically dieses feld geht dort hin
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
    @Mapping(source = "bio", target = "bio")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO); // Entity kennt alle Felder auch das Passwort, weil e sin der Datenbank gespeichert wird.
    // nimmt daten die vom Frontend kommen (UserPostdto) und wandelt sie in user entity um
    // so können sie in der datenbank gespeichert werden, source sagt woher target wohin.

    @Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "status", target = "status")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "creationDate", target = "creationDate")
    @Mapping(source = "gamesWon", target = "gamesWon")
    @Mapping(source = "averageScorePerRound", target = "averageScorePerRound")
    @Mapping(source = "overallRank", target = "overallRank")
	UserGetDTO convertEntityToUserGetDTO(User user);
    // nimmt user aus der datenbank vom backend zum frintend und wandelt ihn in einen usergetdto um,
    // wird dann ans frontend gesendet, passwirt wird weggelassen, wegen security und weil es kein feld dafür gibt

    @Mapping(source = "password", target = "password")
    @Mapping(source = "status", target = "status")
    User convertUserPutDTOtoEntity(UserPutDTO userPutDTO);
    // für Änderung des Passwortes


}

