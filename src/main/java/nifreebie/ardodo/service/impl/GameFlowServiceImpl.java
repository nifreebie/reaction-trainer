package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.GameProperties;
import nifreebie.ardodo.domain.GameSession;
import nifreebie.ardodo.domain.ArithmeticRound;
import nifreebie.ardodo.domain.Result;
import nifreebie.ardodo.domain.RoundResult;
import nifreebie.ardodo.domain.RoundStatus;
import nifreebie.ardodo.domain.SessionStatus;
import nifreebie.ardodo.dto.websocket.GameFinishedMessage;
import nifreebie.ardodo.dto.websocket.RoundStartMessage;
import nifreebie.ardodo.repository.GameSessionRepository;
import nifreebie.ardodo.repository.ArithmeticRoundRepository;
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
    private final ArithmeticRoundRepository arithmeticRoundRepository;
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
            Integer enteredAnswer,
            Integer answerTimeMs
    ) {
        GameSession session = findSession(sessionId);
        validateActiveSession(session, deviceId);

        ArithmeticRound round = findRound(sessionId, roundNumber);
        if (round.getStatus() == RoundStatus.COMPLETED) {
            return;
        }

        RoundResult normalizedResult = determineResult(round, enteredAnswer, answerTimeMs);

        round.setEnteredAnswer(enteredAnswer);
        round.setAnsweredAt(LocalDateTime.now());
        round.setAnswerTimeMs(answerTimeMs);
        round.setResult(normalizedResult);
        round.setStatus(RoundStatus.COMPLETED);
        arithmeticRoundRepository.save(round);

        updateSessionStats(session, answerTimeMs, normalizedResult);

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

    private ArithmeticRound findRound(UUID sessionId, int roundNumber) {
        return arithmeticRoundRepository.findBySessionIdAndRoundNumber(sessionId, roundNumber)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));
    }

    private void validateSessionSettings(GameSession session) {
        if (session.getRoundsCount() == null || session.getRoundsCount() <= 0) {
            throw new IllegalArgumentException("Invalid roundsCount");
        }

        if (session.getTimeoutMs() == null || session.getTimeoutMs() <= 0) {
            throw new IllegalArgumentException("Invalid timeoutMs");
        }

        if (gameProperties.getNumberMin() < -99 || gameProperties.getNumberMax() > 99
                || gameProperties.getNumberMin() > gameProperties.getNumberMax()) {
            throw new IllegalArgumentException("Number range must be within -99..99");
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
        int firstNumber = randomNumber();
        int secondNumber = randomNumber();

        ArithmeticRound round = new ArithmeticRound();
        round.setSession(session);
        round.setRoundNumber(roundNumber);
        round.setFirstNumber(firstNumber);
        round.setSecondNumber(secondNumber);
        round.setCorrectAnswer(firstNumber + secondNumber);
        round.setShownAt(LocalDateTime.now());
        round.setTimeoutMs(session.getTimeoutMs());
        round.setStatus(RoundStatus.PLANNED);
        arithmeticRoundRepository.save(round);

        deviceMessageSender.sendToDevice(
                session.getDevice().getId(),
                new RoundStartMessage(
                        "round_start",
                        session.getId(),
                        roundNumber,
                        firstNumber,
                        secondNumber,
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
                        zero(session.getAvgAnswerTimeMs()),
                        zero(session.getBestAnswerTimeMs()),
                        zero(session.getCorrectAnswersCount()),
                        zero(session.getIncorrectAnswersCount()),
                        zero(session.getMissedAnswersCount())
                )
        );
    }

    private void saveResult(GameSession session) {
        if (session.getBestAnswerTimeMs() == null) {
            return;
        }

        Result result = new Result();
        result.setPlayer(session.getPlayer());
        result.setDeviceId(session.getDevice().getId());
        result.setTimeMs(session.getBestAnswerTimeMs());
        resultRepository.save(result);
    }

    private void updateSessionStats(GameSession session, Integer answerTimeMs, RoundResult result) {
        switch (result) {
            case INCORRECT -> session.setIncorrectAnswersCount(zero(session.getIncorrectAnswersCount()) + 1);
            case MISS -> session.setMissedAnswersCount(zero(session.getMissedAnswersCount()) + 1);
            case CORRECT -> {
                session.setCorrectAnswersCount(zero(session.getCorrectAnswersCount()) + 1);

                if (answerTimeMs != null) {
                    int totalAnswerTime = zero(session.getTotalAnswerTimeMs()) + answerTimeMs;
                    session.setTotalAnswerTimeMs(totalAnswerTime);
                    session.setAvgAnswerTimeMs(totalAnswerTime / session.getCorrectAnswersCount());

                    if (session.getBestAnswerTimeMs() == null || answerTimeMs < session.getBestAnswerTimeMs()) {
                        session.setBestAnswerTimeMs(answerTimeMs);
                    }
                }
            }
        }
    }

    private RoundResult determineResult(ArithmeticRound round, Integer enteredAnswer, Integer answerTimeMs) {
        if (enteredAnswer == null || answerTimeMs == null || answerTimeMs < 0
                || answerTimeMs > round.getTimeoutMs()) {
            return RoundResult.MISS;
        }

        if (!enteredAnswer.equals(round.getCorrectAnswer())) {
            return RoundResult.INCORRECT;
        }

        return RoundResult.CORRECT;
    }

    private int randomNumber() {
        return gameProperties.getNumberMin() + random.nextInt(gameProperties.randomNumberRange());
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
