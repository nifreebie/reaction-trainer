package nifreebie.ardodo.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nifreebie.ardodo.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevicePresenceCleanupJob {

    private final DeviceRepository deviceRepository;

    @Value("${app.device.heartbeat-timeout-seconds}")
    private long heartbeatTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.device.offline-check-delay-ms}")
    @Transactional
    public void markStaleDevicesOffline() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
        int updated = deviceRepository.markStaleDevicesOffline(threshold);

        if (updated > 0) {
            log.info("Marked {} stale device(s) offline", updated);
        }
    }
}
