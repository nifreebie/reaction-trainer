package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.domain.Player;
import nifreebie.ardodo.domain.Result;
import nifreebie.ardodo.dto.request.ResultCreateRequest;
import nifreebie.ardodo.dto.response.LeaderboardEntryResponse;
import nifreebie.ardodo.dto.response.ResultResponse;
import nifreebie.ardodo.repository.PlayerRepository;
import nifreebie.ardodo.repository.ResultRepository;
import nifreebie.ardodo.service.ResultService;
import nifreebie.ardodo.util.BadRequestException;
import nifreebie.ardodo.util.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final PlayerRepository playerRepository;
    private final ResultRepository resultRepository;

    @Override
    @Transactional
    public UUID create(ResultCreateRequest request) {
        validateCreateRequest(request);

        Result result = new Result();
        result.setPlayer(resolvePlayer(request));
        result.setDeviceId(request.deviceId());
        result.setTimeMs(request.timeMs());

        return resultRepository.save(result).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaders(int limit) {
        return resultRepository.findLeaders(PageRequest.of(0, normalizeLimit(limit)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultResponse> getPlayerResults(UUID playerId, int limit) {
        return resultRepository.findByPlayerIdOrderByCreatedAtDesc(playerId, PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResultResponse getBestResult(UUID playerId) {
        return resultRepository.findFirstByPlayerIdOrderByTimeMsAscCreatedAtAsc(playerId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Result not found"));
    }

    private void validateCreateRequest(ResultCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        if (request.timeMs() == null || request.timeMs() <= 0) {
            throw new BadRequestException("timeMs must be positive");
        }

        if (isBlank(request.playerId()) && isBlank(request.name())) {
            throw new BadRequestException("playerId or name is required");
        }
    }

    private Player resolvePlayer(ResultCreateRequest request) {
        if (!isBlank(request.playerId())) {
            UUID playerId = parsePlayerId(request.playerId());
            return playerRepository.findById(playerId)
                    .orElseThrow(() -> new NotFoundException("Player not found"));
        }

        return playerRepository.findByName(request.name())
                .orElseThrow(() -> new NotFoundException("Player not found"));
    }

    private UUID parsePlayerId(String rawPlayerId) {
        try {
            return UUID.fromString(rawPlayerId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("playerId must be UUID");
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResultResponse toResponse(Result result) {
        return new ResultResponse(
                result.getId(),
                result.getPlayer().getId(),
                result.getPlayer().getName(),
                result.getTimeMs(),
                result.getDeviceId(),
                result.getCreatedAt()
        );
    }
}
