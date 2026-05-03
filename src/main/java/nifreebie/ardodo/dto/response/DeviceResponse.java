package nifreebie.ardodo.dto.response;

import java.time.LocalDateTime;

public record DeviceResponse(
        String id,
        String name,
        String firmwareVersion,
        Boolean online,
        LocalDateTime lastSeenAt,
        LocalDateTime registeredAt
) {
}
