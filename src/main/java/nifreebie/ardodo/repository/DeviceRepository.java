package nifreebie.ardodo.repository;

import nifreebie.ardodo.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, String> {
    boolean existsById(String id);
    Optional<Device> findByDeviceToken(String deviceTokenHash);
    List<Device> findByPlayerId(UUID playerId);
    Optional<Device> findByIdAndPlayerId(String id, UUID playerId);
    List<Device> findByIsOnlineTrue();

    @Modifying
    @Query("UPDATE Device d SET d.isOnline = false WHERE d.isOnline = true AND d.lastSeenAt < :threshold")
    int markStaleDevicesOffline(LocalDateTime threshold);
}
