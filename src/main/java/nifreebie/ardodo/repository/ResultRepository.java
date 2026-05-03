package nifreebie.ardodo.repository;

import nifreebie.ardodo.domain.Result;
import nifreebie.ardodo.dto.response.LeaderboardEntryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResultRepository extends JpaRepository<Result, UUID> {

    @Query("""
            SELECT new nifreebie.ardodo.dto.response.LeaderboardEntryResponse(
                r.id,
                r.player.id,
                r.player.name,
                r.timeMs,
                r.deviceId,
                r.createdAt
            )
            FROM Result r
            ORDER BY r.timeMs ASC, r.createdAt ASC
            """)
    List<LeaderboardEntryResponse> findLeaders(Pageable pageable);

    List<Result> findByPlayerIdOrderByCreatedAtDesc(UUID playerId, Pageable pageable);

    Optional<Result> findFirstByPlayerIdOrderByTimeMsAscCreatedAtAsc(UUID playerId);

    long countByPlayerId(UUID playerId);
}
