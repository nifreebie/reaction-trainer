package nifreebie.ardodo.repository;

import nifreebie.ardodo.domain.ReactionRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReactionRoundRepository extends JpaRepository<ReactionRound, UUID> {
    Optional<ReactionRound> findBySessionIdAndRoundNumber(UUID sessionId, Integer roundNumber);

    List<ReactionRound> findBySessionIdOrderByRoundNumberAsc(UUID sessionId);
}
