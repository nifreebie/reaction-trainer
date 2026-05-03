package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.domain.Player;
import nifreebie.ardodo.dto.PlayerDTO;
import nifreebie.ardodo.dto.request.UpdatePlayerRequest;
import nifreebie.ardodo.dto.response.PlayerStatsResponse;
import nifreebie.ardodo.mapper.PlayerMapper;
import nifreebie.ardodo.repository.GameSessionRepository;
import nifreebie.ardodo.repository.PlayerRepository;
import nifreebie.ardodo.repository.ResultRepository;
import nifreebie.ardodo.service.PlayerService;
import nifreebie.ardodo.util.BadRequestException;
import nifreebie.ardodo.util.DuplicateUsernameException;
import nifreebie.ardodo.util.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {
    private final PlayerRepository playerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final ResultRepository resultRepository;
    private final PlayerMapper playerMapper;

    @Override
    public PlayerDTO getUserByName(String name) {
        return playerRepository.findByName(name)
                .map(playerMapper::toPlayerDTO)
                .orElseThrow();
    }

    @Override
    public PlayerDTO getUserById(UUID id) {
        return playerRepository.findById(id)
                .map(playerMapper::toPlayerDTO)
                .orElseThrow();
    }

    @Override
    public boolean isExistsById(UUID id) {
        return playerRepository.existsById(id);
    }

    @Override
    public boolean isExistsByName(String name) {
        return playerRepository.existsByName(name);
    }

    @Override
    @Transactional
    public PlayerDTO update(UUID id, UpdatePlayerRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("name is required");
        }

        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Player not found"));

        String newName = request.name().trim();
        if (!newName.equals(player.getName()) && playerRepository.existsByName(newName)) {
            throw new DuplicateUsernameException("Username already exists");
        }

        player.setName(newName);
        return playerMapper.toPlayerDTO(player);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerStatsResponse getStats(UUID id) {
        var sessions = gameSessionRepository.findByPlayerId(id);
        long resultsCount = resultRepository.countByPlayerId(id);
        Integer bestResultTime = resultRepository.findFirstByPlayerIdOrderByTimeMsAscCreatedAtAsc(id)
                .map(result -> result.getTimeMs())
                .orElse(null);

        int hits = sessions.stream().mapToInt(session -> zero(session.getHitsCount())).sum();
        int misses = sessions.stream().mapToInt(session -> zero(session.getMissesCount())).sum();
        int wrongButtons = sessions.stream().mapToInt(session -> zero(session.getWrongButtonsCount())).sum();
        int falseStarts = sessions.stream().mapToInt(session -> zero(session.getFalseStartsCount())).sum();

        Integer bestReaction = sessions.stream()
                .map(session -> session.getBestReactionMs())
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);

        int totalHitsWithReaction = sessions.stream()
                .filter(session -> session.getTotalReactionMs() != null && session.getHitsCount() != null)
                .mapToInt(session -> session.getHitsCount())
                .sum();
        int totalReaction = sessions.stream()
                .mapToInt(session -> zero(session.getTotalReactionMs()))
                .sum();
        Integer avgReaction = totalHitsWithReaction == 0 ? null : totalReaction / totalHitsWithReaction;

        return new PlayerStatsResponse(
                sessions.size(),
                resultsCount,
                bestResultTime,
                bestReaction,
                avgReaction,
                hits,
                misses,
                wrongButtons,
                falseStarts
        );
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
