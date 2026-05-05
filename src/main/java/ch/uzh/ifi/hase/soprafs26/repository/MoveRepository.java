package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    // finds all moves of a session in chronological order
    List<Move> findBySessionIdOrderByTimestampAsc(String sessionId);
    
    // finds all moves of a specific user in a session
    List<Move> findBySessionIdAndUserId(String sessionId, Long userId);
}