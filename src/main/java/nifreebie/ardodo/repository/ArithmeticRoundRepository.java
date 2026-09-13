package nifreebie.ardodo.repository;

import nifreebie.ardodo.domain.ArithmeticRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArithmeticRoundRepository extends JpaRepository<ArithmeticRound, UUID> {
    Optional<ArithmeticRound> findBySessionIdAndRoundNumber(UUID sessionId, Integer roundNumber);

    List<ArithmeticRound> findBySessionIdOrderByRoundNumberAsc(UUID sessionId);
}
