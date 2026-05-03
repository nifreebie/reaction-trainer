package nifreebie.ardodo.dto.response;

import nifreebie.ardodo.domain.GameMode;
import nifreebie.ardodo.domain.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String deviceId,
        String deviceName,
        SessionStatus status,
        GameMode mode,
        Integer currentRound,
        Integer roundsCount,
        Integer timeoutMs,
        Integer avgReactionMs,
        Integer bestReactionMs,
        Integer hitsCount,
        Integer missesCount,
        Integer wrongButtonsCount,
        Integer falseStartsCount,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
