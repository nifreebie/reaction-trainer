package nifreebie.ardodo.dto.websocket;

import java.util.UUID;

public record GameFinishedMessage(
        String type,
        UUID sessionId,
        int avgReactionMs,
        int bestReactionMs,
        int missesCount,
        int wrongButtonsCount,
        int falseStartsCount
) {}