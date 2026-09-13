package nifreebie.ardodo.repository;

import nifreebie.ardodo.domain.Result;
import nifreebie.ardodo.dto.response.LeaderboardEntryProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResultRepository extends JpaRepository<Result, UUID> {

    @Query(value = """
            SELECT
                ranked.id AS "resultId",
                ranked.player_id AS "playerId",
                p.name AS "playerName",
                ranked.time_ms AS "timeMs",
                ranked.device_id AS "deviceId",
                ranked.created_at AS "createdAt"
            FROM (
                SELECT
                    r.*,
                    ROW_NUMBER() OVER (
                        PARTITION BY r.player_id
                        ORDER BY r.time_ms ASC, r.created_at ASC, r.id ASC
                    ) AS rn
                FROM results r
            ) ranked
            JOIN players p ON p.id = ranked.player_id
            WHERE ranked.rn = 1
            ORDER BY ranked.time_ms ASC, ranked.created_at ASC, ranked.id ASC
            """, nativeQuery = true)
    List<LeaderboardEntryProjection> findBestLeaders(Pageable pageable);

    List<Result> findByPlayerIdOrderByCreatedAtDesc(UUID playerId, Pageable pageable);

    Optional<Result> findFirstByPlayerIdOrderByTimeMsAscCreatedAtAsc(UUID playerId);

    long countByPlayerId(UUID playerId);
}
