package nifreebie.ardodo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeaderboardEntryResponse(
        UUID resultId,
        UUID playerId,
        String playerName,
        Integer timeMs,
        String deviceId,
        LocalDateTime createdAt
) {
}
