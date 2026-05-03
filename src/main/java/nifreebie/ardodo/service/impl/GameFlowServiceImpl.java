package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.GameProperties;
import nifreebie.ardodo.domain.GameSession;
import nifreebie.ardodo.domain.ReactionRound;
import nifreebie.ardodo.domain.Result;
import nifreebie.ardodo.domain.RoundResult;
import nifreebie.ardodo.domain.RoundStatus;
import nifreebie.ardodo.domain.SessionStatus;
import nifreebie.ardodo.dto.websocket.GameFinishedMessage;
import nifreebie.ardodo.dto.websocket.RoundStartMessage;
import nifreebie.ardodo.repository.GameSessionRepository;
import nifreebie.ardodo.repository.ReactionRoundRepository;
import nifreebie.ardodo.repository.ResultRepository;
import nifreebie.ardodo.service.DeviceMessageSender;
import nifreebie.ardodo.service.GameFlowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameFlowServiceImpl implements GameFlowService {

    private static final String SESSION_NOT_FOUND = "Session not found";

    private final GameSessionRepository gameSessionRepository;
    private final ReactionRoundRepository reactionRoundRepository;
    private final ResultRepository resultRepository;
    private final DeviceMessageSender deviceMessageSender;
    private final GameProperties gameProperties;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    @Override
    public void startGame(UUID sessionId) {
        GameSession session = findSession(sessionId);
        if (session.getStatus() != SessionStatus.WAITING) {
            throw new IllegalStateException("Session is not waiting for start");
        }

        validateSessionSettings(session);
        session.setStatus(SessionStatus.ACTIVE);
        session.setCurrentRound(1);

        sendRoundStart(session);
    }

    @Transactional
    @Override
    public void handleRoundResult(
            String deviceId,
            UUID sessionId,
            int roundNumber,
            Integer pressedButton,
            Integer reactionTimeMs,
            RoundResult result
    ) {
        GameSession session = findSession(sessionId);
        validateActiveSession(session, deviceId);

        ReactionRound round = findRound(sessionId, roundNumber);
        if (round.getStatus() == RoundStatus.COMPLETED) {
            return;
        }

        RoundResult normalizedResult = normalizeResult(round, pressedButton, reactionTimeMs, result);

        round.setPressedButton(pressedButton);
        round.setPressedAt(LocalDateTime.now());
        round.setReactionTimeMs(reactionTimeMs);
        round.setResult(normalizedResult);
        round.setStatus(RoundStatus.COMPLETED);
        reactionRoundRepository.save(round);

        updateSessionStats(session, reactionTimeMs, normalizedResult);

        if (roundNumber >= session.getRoundsCount()) {
            finishGame(session);
            return;
        }

        session.setCurrentRound(roundNumber + 1);
        sendRoundStart(session);
    }

    private GameSession findSession(UUID sessionId) {
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(SESSION_NOT_FOUND));
    }

    private ReactionRound findRound(UUID sessionId, int roundNumber) {
        return reactionRoundRepository.findBySessionIdAndRoundNumber(sessionId, roundNumber)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));
    }

    private void validateSessionSettings(GameSession session) {
        if (session.getRoundsCount() == null || session.getRoundsCount() <= 0) {
            throw new IllegalArgumentException("Invalid roundsCount");
        }

        if (session.getTimeoutMs() == null || session.getTimeoutMs() <= 0) {
            throw new IllegalArgumentException("Invalid timeoutMs");
        }
    }

    private void validateActiveSession(GameSession session, String deviceId) {
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Session is not active");
        }

        if (!session.getDevice().getId().equals(deviceId)) {
            throw new IllegalArgumentException("Device does not belong to this session");
        }
    }

    private void sendRoundStart(GameSession session) {
        int roundNumber = session.getCurrentRound();
        int targetButton = random.nextInt(gameProperties.getTargetButtonsCount()) + 1;
        int stimulusDelayMs = gameProperties.getStimulusDelayMinMs()
                + random.nextInt(gameProperties.randomStimulusDelayRange());

        ReactionRound round = new ReactionRound();
        round.setSession(session);
        round.setRoundNumber(roundNumber);
        round.setTargetButton(targetButton);
        round.setStimulusDelayMs(stimulusDelayMs);
        round.setTimeoutMs(session.getTimeoutMs());
        round.setStatus(RoundStatus.PLANNED);
        reactionRoundRepository.save(round);

        deviceMessageSender.sendToDevice(
                session.getDevice().getId(),
                new RoundStartMessage(
                        "round_start",
                        session.getId(),
                        roundNumber,
                        targetButton,
                        stimulusDelayMs,
                        session.getTimeoutMs()
                )
        );
    }

    private void finishGame(GameSession session) {
        session.setStatus(SessionStatus.FINISHED);
        session.setEndedAt(LocalDateTime.now());
        saveResult(session);

        deviceMessageSender.sendToDevice(
                session.getDevice().getId(),
                new GameFinishedMessage(
                        "game_finished",
                        session.getId(),
                        zero(session.getAvgReactionMs()),
                        zero(session.getBestReactionMs()),
                        zero(session.getMissesCount()),
                        zero(session.getWrongButtonsCount()),
                        zero(session.getFalseStartsCount())
                )
        );
    }

    private void saveResult(GameSession session) {
        if (session.getBestReactionMs() == null) {
            return;
        }

        Result result = new Result();
        result.setPlayer(session.getPlayer());
        result.setDeviceId(session.getDevice().getId());
        result.setTimeMs(session.getBestReactionMs());
        resultRepository.save(result);
    }

    private void updateSessionStats(GameSession session, Integer reactionTimeMs, RoundResult result) {
        switch (result) {
            case FALSE_START -> session.setFalseStartsCount(zero(session.getFalseStartsCount()) + 1);
            case WRONG_BUTTON -> session.setWrongButtonsCount(zero(session.getWrongButtonsCount()) + 1);
            case MISS -> session.setMissesCount(zero(session.getMissesCount()) + 1);
            case HIT -> {
                session.setHitsCount(zero(session.getHitsCount()) + 1);

                if (reactionTimeMs != null) {
                    int totalReaction = zero(session.getTotalReactionMs()) + reactionTimeMs;
                    session.setTotalReactionMs(totalReaction);
                    session.setAvgReactionMs(totalReaction / session.getHitsCount());

                    if (session.getBestReactionMs() == null || reactionTimeMs < session.getBestReactionMs()) {
                        session.setBestReactionMs(reactionTimeMs);
                    }
                }
            }
        }
    }

    private RoundResult normalizeResult(
            ReactionRound round,
            Integer pressedButton,
            Integer reactionTimeMs,
            RoundResult reportedResult
    ) {
        if (reportedResult == RoundResult.FALSE_START) {
            return RoundResult.FALSE_START;
        }

        if (pressedButton == null || reactionTimeMs == null || reactionTimeMs > round.getTimeoutMs()) {
            return RoundResult.MISS;
        }

        if (!pressedButton.equals(round.getTargetButton())) {
            return RoundResult.WRONG_BUTTON;
        }

        return RoundResult.HIT;
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
