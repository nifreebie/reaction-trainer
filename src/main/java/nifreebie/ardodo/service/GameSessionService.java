package nifreebie.ardodo.service;

import nifreebie.ardodo.dto.response.RoundResponse;
import nifreebie.ardodo.dto.response.SessionResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionService {
    Optional<SessionResponse> getCurrent(UUID playerId);

    List<SessionResponse> getPlayerSessions(UUID playerId, int limit);

    SessionResponse getSession(UUID playerId, UUID sessionId);

    List<RoundResponse> getSessionRounds(UUID playerId, UUID sessionId);
}
