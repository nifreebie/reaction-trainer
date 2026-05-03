package nifreebie.ardodo.service;

import nifreebie.ardodo.domain.RoundResult;

import java.util.UUID;

public interface GameFlowService {
    void startGame(UUID sessionId);

    void handleRoundResult(
            String deviceId,
            UUID sessionId,
            int roundNumber,
            Integer pressedButton,
            Integer reactionTimeMs,
            RoundResult result
    );
}
