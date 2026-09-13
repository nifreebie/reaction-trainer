package nifreebie.ardodo.dto.websocket;

import java.util.UUID;

public record RoundStartMessage(
        String type,
        UUID sessionId,
        int roundNumber,
        int firstNumber,
        int secondNumber,
        int timeoutMs
) {}
