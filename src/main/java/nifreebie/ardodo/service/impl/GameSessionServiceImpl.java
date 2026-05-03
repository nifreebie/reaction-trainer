package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.domain.GameSession;
import nifreebie.ardodo.domain.ReactionRound;
import nifreebie.ardodo.domain.SessionStatus;
import nifreebie.ardodo.dto.response.RoundResponse;
import nifreebie.ardodo.dto.response.SessionResponse;
import nifreebie.ardodo.repository.GameSessionRepository;
import nifreebie.ardodo.repository.ReactionRoundRepository;
import nifreebie.ardodo.service.GameSessionService;
import nifreebie.ardodo.util.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameSessionServiceImpl implements GameSessionService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final List<SessionStatus> CURRENT_STATUSES = List.of(SessionStatus.WAITING, SessionStatus.ACTIVE);

    private final GameSessionRepository gameSessionRepository;
    private final ReactionRoundRepository reactionRoundRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<SessionResponse> getCurrent(UUID playerId) {
        return gameSessionRepository.findFirstByPlayerIdAndStatusInOrderByStartedAtDesc(playerId, CURRENT_STATUSES)
                .map(this::toSessionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getPlayerSessions(UUID playerId, int limit) {
        return gameSessionRepository.findByPlayerIdOrderByStartedAtDesc(playerId, PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID playerId, UUID sessionId) {
        return toSessionResponse(findPlayerSession(playerId, sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundResponse> getSessionRounds(UUID playerId, UUID sessionId) {
        findPlayerSession(playerId, sessionId);
        return reactionRoundRepository.findBySessionIdOrderByRoundNumberAsc(sessionId)
                .stream()
                .map(this::toRoundResponse)
                .toList();
    }

    private GameSession findPlayerSession(UUID playerId, UUID sessionId) {
        return gameSessionRepository.findByIdAndPlayerId(sessionId, playerId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private SessionResponse toSessionResponse(GameSession session) {
        return new SessionResponse(
                session.getId(),
                session.getDevice().getId(),
                session.getDevice().getName(),
                session.getStatus(),
                session.getMode(),
                session.getCurrentRound(),
                session.getRoundsCount(),
                session.getTimeoutMs(),
                session.getAvgReactionMs(),
                session.getBestReactionMs(),
                session.getHitsCount(),
                session.getMissesCount(),
                session.getWrongButtonsCount(),
                session.getFalseStartsCount(),
                session.getStartedAt(),
                session.getEndedAt()
        );
    }

    private RoundResponse toRoundResponse(ReactionRound round) {
        return new RoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getTargetButton(),
                round.getStimulusDelayMs(),
                round.getTimeoutMs(),
                round.getPressedButton(),
                round.getReactionTimeMs(),
                round.getStatus(),
                round.getResult(),
                round.getStimulusAt(),
                round.getPressedAt()
        );
    }
}
