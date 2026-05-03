package nifreebie.ardodo.repository;

import jakarta.persistence.LockModeType;
import nifreebie.ardodo.domain.PairCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PairCodeRepository extends JpaRepository<PairCode, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PairCode> findByCodeAndUsedFalseAndExpiresAtAfter(String code, LocalDateTime now);

    Optional<PairCode> findByCode(String code);

    Optional<PairCode> findFirstByPlayerIdAndUsedFalseAndExpiresAtAfterOrderByExpiresAtDesc(UUID playerId, LocalDateTime now);

    Optional<PairCode> findByCodeAndPlayerId(String code, UUID playerId);

    boolean existsByCodeAndUsedFalseAndExpiresAtAfter(String code, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PairCode p WHERE p.expiresAt < CURRENT_TIMESTAMP OR p.used = true")
    void deleteExpiredOrUsed();
}
