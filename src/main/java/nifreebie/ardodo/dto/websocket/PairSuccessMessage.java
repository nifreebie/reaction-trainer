package nifreebie.ardodo.dto.websocket;

import java.util.UUID;

public record PairSuccessMessage(
        String type,
        UUID sessionId,
        String playerName
) {}