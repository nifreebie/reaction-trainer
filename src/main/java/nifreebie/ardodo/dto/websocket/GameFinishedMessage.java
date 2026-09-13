package nifreebie.ardodo.dto.websocket;

import java.util.UUID;

public record GameFinishedMessage(
        String type,
        UUID sessionId,
        int avgAnswerTimeMs,
        int bestAnswerTimeMs,
        int correctAnswersCount,
        int incorrectAnswersCount,
        int missedAnswersCount
) {}
