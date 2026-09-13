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

        int correctAnswers = sessions.stream().mapToInt(session -> zero(session.getCorrectAnswersCount())).sum();
        int incorrectAnswers = sessions.stream().mapToInt(session -> zero(session.getIncorrectAnswersCount())).sum();
        int missedAnswers = sessions.stream().mapToInt(session -> zero(session.getMissedAnswersCount())).sum();

        Integer bestAnswerTime = sessions.stream()
                .map(session -> session.getBestAnswerTimeMs())
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);

        int correctAnswersWithTime = sessions.stream()
                .filter(session -> session.getTotalAnswerTimeMs() != null && session.getCorrectAnswersCount() != null)
                .mapToInt(session -> session.getCorrectAnswersCount())
                .sum();
        int totalAnswerTime = sessions.stream()
                .mapToInt(session -> zero(session.getTotalAnswerTimeMs()))
                .sum();
        Integer avgAnswerTime = correctAnswersWithTime == 0 ? null : totalAnswerTime / correctAnswersWithTime;

        return new PlayerStatsResponse(
                sessions.size(),
                resultsCount,
                bestResultTime,
                bestAnswerTime,
                avgAnswerTime,
                correctAnswers,
                incorrectAnswers,
                missedAnswers
        );
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
