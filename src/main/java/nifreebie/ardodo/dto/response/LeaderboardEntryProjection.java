package nifreebie.ardodo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LeaderboardEntryProjection {
    UUID getResultId();

    UUID getPlayerId();

    String getPlayerName();

    Integer getTimeMs();

    String getDeviceId();

    LocalDateTime getCreatedAt();
}
