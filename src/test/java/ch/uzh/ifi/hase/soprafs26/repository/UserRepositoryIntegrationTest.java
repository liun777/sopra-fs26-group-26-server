package ch.uzh.ifi.hase.soprafs26.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.PersistenceException;

@DataJpaTest
public class UserRepositoryIntegrationTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Test
	public void findByName_success() {
		// given
		User user = new User();
		user.setName("Firstname Lastname");
		user.setUsername("first@last");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");
		user.setPassword("testPassword");
		user.setCreationDate(java.time.LocalDate.now());

		entityManager.persist(user);
		entityManager.flush();

		// when
		User found = userRepository.findByName(user.getName());

		// then
		assertNotNull(found.getId());
		assertEquals(found.getName(), user.getName());
		assertEquals(found.getUsername(), user.getUsername());
		assertEquals(found.getToken(), user.getToken());
		assertEquals(found.getStatus(), user.getStatus());
	}

	@Test
	public void saveUser_duplicateUsername_throwsException() {
    	// given
    	User user1 = new User();
    	user1.setName("User One");
    	user1.setUsername("duplicate");
    	user1.setToken("token1");
    	user1.setPassword("password");
    	user1.setCreationDate(java.time.LocalDate.now());
    	user1.setStatus(UserStatus.OFFLINE);

    	entityManager.persist(user1);
    	entityManager.flush();

    	// when
    	User user2 = new User();
    	user2.setName("User Two");
    	user2.setUsername("duplicate"); 
    	user2.setToken("token2");
    	user2.setPassword("password");
    	user2.setCreationDate(java.time.LocalDate.now());
    	user2.setStatus(UserStatus.OFFLINE);

    	// then
    	assertThrows(PersistenceException.class, () -> {
        	entityManager.persist(user2);
        	entityManager.flush();
    	});
	}
}
