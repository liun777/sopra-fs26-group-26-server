package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.entity.Card;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;

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

    // we cannot use mapping since the api returns a card code but we also need a value and mapping 
    // doesnt know which value a card code implies or what the visibilit should be so we use this 
    // default method to overwrite MapStruct. This allows us to inject our game logic for the cards
    default Card convertCardDTOtoEntity(CardDTO cardDTO) {
        Card card = new Card();
        String code = cardDTO.getCode();
        card.setCode(code);
        card.setVisibility(false);
        char firstChar = code.charAt(0);
        int cardValue = 0;

        switch (firstChar) {
            case 'X':
                cardValue = 0;
                break;
            case 'A':
                cardValue = 1;
                break;
            case '0':
                cardValue = 10;
                break;
            case 'J':
                cardValue = 11;
                break;
            case 'Q':
                cardValue = 12;
                break;
            case 'K':
                cardValue = 13;
                break;
            default:
                // if the first character is a number 1-9 we can simply assign that as value
                cardValue = Character.getNumericValue(firstChar);
                break;
        }
        card.setValue(cardValue);
        return card;
    }

    //MapStruct knows how to convert cardDTOs into cards with the above default method so now we 
    // can simply convert whole lists of cardDTOs into cards
    List<Card> convertCardDTOListtoEntityList(List<CardDTO> cardDTOs);

}

