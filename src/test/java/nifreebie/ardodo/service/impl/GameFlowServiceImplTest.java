package nifreebie.ardodo.service.impl;

import nifreebie.ardodo.config.GameProperties;
import nifreebie.ardodo.domain.ArithmeticRound;
import nifreebie.ardodo.domain.Device;
import nifreebie.ardodo.domain.GameSession;
import nifreebie.ardodo.domain.Player;
import nifreebie.ardodo.domain.RoundResult;
import nifreebie.ardodo.domain.RoundStatus;
import nifreebie.ardodo.domain.SessionStatus;
import nifreebie.ardodo.dto.websocket.RoundStartMessage;
import nifreebie.ardodo.repository.ArithmeticRoundRepository;
import nifreebie.ardodo.repository.GameSessionRepository;
import nifreebie.ardodo.repository.ResultRepository;
import nifreebie.ardodo.service.DeviceMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameFlowServiceImplTest {

    private GameSessionRepository gameSessionRepository;
    private ArithmeticRoundRepository arithmeticRoundRepository;
    private ResultRepository resultRepository;
    private DeviceMessageSender deviceMessageSender;
    private GameFlowServiceImpl service;

    @BeforeEach
    void setUp() {
        gameSessionRepository = mock(GameSessionRepository.class);
        arithmeticRoundRepository = mock(ArithmeticRoundRepository.class);
        resultRepository = mock(ResultRepository.class);
        deviceMessageSender = mock(DeviceMessageSender.class);

        GameProperties properties = new GameProperties();
        properties.setRoundsCount(10);
        properties.setTimeoutMs(30000);
        properties.setNumberMin(-99);
        properties.setNumberMax(99);

        service = new GameFlowServiceImpl(
                gameSessionRepository,
                arithmeticRoundRepository,
                resultRepository,
                deviceMessageSender,
                properties
        );
    }

    @Test
    void startsRoundWithOperandsInsideConfiguredRange() {
        GameSession session = session(SessionStatus.WAITING, 10);
        when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        service.startGame(session.getId());

        ArgumentCaptor<ArithmeticRound> roundCaptor = ArgumentCaptor.forClass(ArithmeticRound.class);
        verify(arithmeticRoundRepository).save(roundCaptor.capture());
        ArithmeticRound round = roundCaptor.getValue();

        assertTrue(round.getFirstNumber() >= -99 && round.getFirstNumber() <= 99);
        assertTrue(round.getSecondNumber() >= -99 && round.getSecondNumber() <= 99);
        assertEquals(round.getFirstNumber() + round.getSecondNumber(), round.getCorrectAnswer());

        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(deviceMessageSender).sendToDevice(any(), messageCaptor.capture());
        RoundStartMessage message = assertInstanceOf(RoundStartMessage.class, messageCaptor.getValue());
        assertEquals(round.getFirstNumber().intValue(), message.firstNumber());
        assertEquals(round.getSecondNumber().intValue(), message.secondNumber());
        assertEquals(30000, message.timeoutMs());
    }

    @ParameterizedTest
    @MethodSource("answerCases")
    void determinesAnswerResultOnBackend(
            Integer enteredAnswer,
            Integer answerTimeMs,
            RoundResult expectedResult,
            int expectedCorrect,
            int expectedIncorrect,
            int expectedMissed
    ) {
        GameSession session = session(SessionStatus.ACTIVE, 1);
        ArithmeticRound round = round(session, 7, -2, 30000);
        when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(arithmeticRoundRepository.findBySessionIdAndRoundNumber(session.getId(), 1))
                .thenReturn(Optional.of(round));

        service.handleRoundResult("device-1", session.getId(), 1, enteredAnswer, answerTimeMs);

        assertEquals(expectedResult, round.getResult());
        assertEquals(RoundStatus.COMPLETED, round.getStatus());
        assertEquals(expectedCorrect, session.getCorrectAnswersCount());
        assertEquals(expectedIncorrect, session.getIncorrectAnswersCount());
        assertEquals(expectedMissed, session.getMissedAnswersCount());
        assertEquals(SessionStatus.FINISHED, session.getStatus());
    }

    private static Stream<Arguments> answerCases() {
        return Stream.of(
                Arguments.of(5, 4200, RoundResult.CORRECT, 1, 0, 0),
                Arguments.of(6, 4200, RoundResult.INCORRECT, 0, 1, 0),
                Arguments.of(null, 30000, RoundResult.MISS, 0, 0, 1),
                Arguments.of(5, 30001, RoundResult.MISS, 0, 0, 1)
        );
    }

    private GameSession session(SessionStatus status, int roundsCount) {
        Player player = new Player();
        player.setId(UUID.randomUUID());

        Device device = new Device();
        device.setId("device-1");

        GameSession session = new GameSession();
        session.setId(UUID.randomUUID());
        session.setPlayer(player);
        session.setDevice(device);
        session.setStatus(status);
        session.setRoundsCount(roundsCount);
        session.setCurrentRound(1);
        session.setTimeoutMs(30000);
        session.setCorrectAnswersCount(0);
        session.setIncorrectAnswersCount(0);
        session.setMissedAnswersCount(0);
        session.setTotalAnswerTimeMs(0);
        return session;
    }

    private ArithmeticRound round(GameSession session, int firstNumber, int secondNumber, int timeoutMs) {
        ArithmeticRound round = new ArithmeticRound();
        round.setSession(session);
        round.setRoundNumber(1);
        round.setFirstNumber(firstNumber);
        round.setSecondNumber(secondNumber);
        round.setCorrectAnswer(firstNumber + secondNumber);
        round.setTimeoutMs(timeoutMs);
        round.setStatus(RoundStatus.PLANNED);
        return round;
    }
}
