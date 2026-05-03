package nifreebie.ardodo.dto.websocket;

import java.util.UUID;

public record StartGameMessage(
        String type,
        UUID sessionId,
        int roundsCount,
        int timeoutMs
) {}