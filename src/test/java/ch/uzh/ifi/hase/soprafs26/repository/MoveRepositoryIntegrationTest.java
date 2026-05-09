package ch.uzh.ifi.hase.soprafs26.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.entity.Move;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class MoveRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MoveRepository moveRepository;

    // integration test with real database persistence
    @Test
    public void isPublic_defaultFalse_correctPersistence() {
        Move defaultMove = new Move();
        defaultMove.setSessionId("S-DEF");
        defaultMove.setUserId(1L);
        defaultMove.setActionType("DRAW");
        entityManager.persist(defaultMove);
        entityManager.flush();
        entityManager.clear();

        Move reloadedDefault = moveRepository.findById(defaultMove.getId()).orElseThrow();
        assertNotNull(reloadedDefault.getIsPublic());
        assertEquals(false, reloadedDefault.getIsPublic());

        Move publicMove = new Move();
        publicMove.setSessionId("S-PUB");
        publicMove.setUserId(2L);
        publicMove.setActionType("PEEK");
        publicMove.setIsPublic(true);
        entityManager.persist(publicMove);
        entityManager.flush();
        entityManager.clear();

        Move reloadedPublicMove = moveRepository.findById(publicMove.getId()).orElseThrow();
        assertEquals(true, reloadedPublicMove.getIsPublic());
    }
}
