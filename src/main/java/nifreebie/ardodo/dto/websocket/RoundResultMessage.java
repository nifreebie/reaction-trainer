package nifreebie.ardodo.dto.websocket;

import java.util.UUID;

public record RoundResultMessage(
        String type,
        UUID sessionId,
        int roundNumber,
        Integer pressedButton,
        Integer reactionTimeMs,
        String result
) {}