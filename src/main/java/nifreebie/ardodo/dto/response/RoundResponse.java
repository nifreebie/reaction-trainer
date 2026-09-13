package nifreebie.ardodo.dto.response;

import nifreebie.ardodo.domain.RoundResult;
import nifreebie.ardodo.domain.RoundStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoundResponse(
        UUID id,
        Integer roundNumber,
        Integer firstNumber,
        Integer secondNumber,
        Integer correctAnswer,
        Integer timeoutMs,
        Integer enteredAnswer,
        Integer answerTimeMs,
        RoundStatus status,
        RoundResult result,
        LocalDateTime shownAt,
        LocalDateTime answeredAt
) {
}
