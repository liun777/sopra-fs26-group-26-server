package ch.uzh.ifi.hase.soprafs26.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

// Datenbankzugriffsschicht - verbindung zwischen service und Datenbank
// Controller empfängt request ->Service macht logik ->Repository greift auf datenbank zu
@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, Long> {
	User findByName(String name);

	User findByUsername(String username);

	User findByToken(String token);  // for the lobby

	List<User> findByStatusNot(UserStatus status);

	List<User> findByLastHeartbeatBeforeAndStatusNot(Instant cutoff, UserStatus status);
}

