package nifreebie.ardodo.dto.websocket;

import java.util.UUID;

public record PairingResult(
        UUID sessionId,
        String playerName
) {}