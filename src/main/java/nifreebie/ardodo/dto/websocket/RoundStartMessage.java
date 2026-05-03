package nifreebie.ardodo.dto.websocket;

import java.util.UUID;

public record RoundStartMessage(
        String type,
        UUID sessionId,
        int roundNumber,
        int targetButton,
        int stimulusDelayMs,
        int timeoutMs
) {}