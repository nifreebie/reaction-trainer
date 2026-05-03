package nifreebie.ardodo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResultResponse(
        UUID id,
        UUID playerId,
        String playerName,
        Integer timeMs,
        String deviceId,
        LocalDateTime createdAt
) {
}
