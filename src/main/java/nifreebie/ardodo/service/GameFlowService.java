package nifreebie.ardodo.service;

import java.util.UUID;

public interface GameFlowService {
    void startGame(UUID sessionId);

    void handleRoundResult(
            String deviceId,
            UUID sessionId,
            int roundNumber,
            Integer enteredAnswer,
            Integer answerTimeMs
    );
}
