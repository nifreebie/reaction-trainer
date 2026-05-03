package nifreebie.ardodo.dto.response;

import java.time.LocalDateTime;

public record PairCodeStatusResponse(
        String code,
        boolean used,
        boolean expired,
        String deviceId,
        LocalDateTime expiresAt,
        LocalDateTime usedAt
) {
}
