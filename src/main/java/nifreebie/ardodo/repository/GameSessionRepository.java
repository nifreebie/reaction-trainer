package nifreebie.ardodo.repository;

import nifreebie.ardodo.domain.GameSession;
import nifreebie.ardodo.domain.SessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {
    Optional<GameSession> findByIdAndPlayerId(UUID id, UUID playerId);

    Optional<GameSession> findFirstByPlayerIdAndStatusInOrderByStartedAtDesc(
            UUID playerId,
            Collection<SessionStatus> statuses
    );

    List<GameSession> findByPlayerIdOrderByStartedAtDesc(UUID playerId, Pageable pageable);

    List<GameSession> findByPlayerId(UUID playerId);

    boolean existsByDeviceId(String deviceId);
}
