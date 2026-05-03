package nifreebie.ardodo.dto.response;

import nifreebie.ardodo.domain.RoundResult;
import nifreebie.ardodo.domain.RoundStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoundResponse(
        UUID id,
        Integer roundNumber,
        Integer targetButton,
        Integer stimulusDelayMs,
        Integer timeoutMs,
        Integer pressedButton,
        Integer reactionTimeMs,
        RoundStatus status,
        RoundResult result,
        LocalDateTime stimulusAt,
        LocalDateTime pressedAt
) {
}
