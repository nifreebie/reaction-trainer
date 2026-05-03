package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.domain.PairCode;
import nifreebie.ardodo.domain.Player;
import nifreebie.ardodo.dto.response.PairCodeResponse;
import nifreebie.ardodo.dto.response.PairCodeStatusResponse;
import nifreebie.ardodo.repository.PairCodeRepository;
import nifreebie.ardodo.repository.PlayerRepository;
import nifreebie.ardodo.service.PairCodeService;
import nifreebie.ardodo.util.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@RequiredArgsConstructor
public class PairCodeServiceImpl implements PairCodeService {

    private final BlockingQueue<String> codeQueue = new LinkedBlockingQueue<>();

    private final PairCodeRepository pairCodeRepository;
    private final PlayerRepository playerRepository;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.pair-code.length}")
    private int CODE_LENGTH;

    @Value("${app.pair-code.pool-size}")
    private int POOL_SIZE;

    @Value("${app.pair-code.ttl-seconds}")
    private int ttlSeconds;

    @Transactional
    @Override
    public PairCodeResponse generateCode(UUID playerId) {
        Player player = findPlayer(playerId);
        String code = pollPreparedCode();
        pairCodeRepository.save(createPairCode(code, player));
        return new PairCodeResponse(code, ttlSeconds);
    }

    @Override
    public synchronized void generateBatch() {
        int generated = 0;

        while (generated < POOL_SIZE) {
            String code = generateCodeValue();

            if (isAvailable(code)) {
                codeQueue.offer(code);
                generated++;
            }
        }
    }

    private Player findPlayer(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    private String pollPreparedCode() {
        if (codeQueue.isEmpty()) {
            generateBatch();
        }

        return Optional.ofNullable(codeQueue.poll())
                .orElseThrow(() -> new IllegalStateException("Pair code pool is empty"));
    }

    private PairCode createPairCode(String code, Player player) {
        return PairCode.builder()
                .code(code)
                .player(player)
                .expiresAt(LocalDateTime.now().plusSeconds(ttlSeconds))
                .used(false)
                .build();
    }

    private String generateCodeValue() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int value = random.nextInt(bound);
        return String.format("%0" + CODE_LENGTH + "d", value);
    }

    private boolean isAvailable(String code) {
        return !codeQueue.contains(code)
                && !pairCodeRepository.existsByCodeAndUsedFalseAndExpiresAtAfter(code, LocalDateTime.now());
    }

    @Transactional
    @Override
    public void cleanup() {
        pairCodeRepository.deleteExpiredOrUsed();
    }

    @Override
    public boolean hasPrepared(int minCount) {
        return codeQueue.size() >= minCount;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PairCodeStatusResponse> getActive(UUID playerId) {
        return pairCodeRepository.findFirstByPlayerIdAndUsedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                        playerId,
                        LocalDateTime.now()
                )
                .map(this::toStatusResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PairCodeStatusResponse getStatus(UUID playerId, String code) {
        return pairCodeRepository.findByCodeAndPlayerId(code, playerId)
                .map(this::toStatusResponse)
                .orElseThrow(() -> new NotFoundException("Pair code not found"));
    }

    @Override
    @Transactional
    public void cancel(UUID playerId, String code) {
        PairCode pairCode = pairCodeRepository.findByCodeAndPlayerId(code, playerId)
                .orElseThrow(() -> new NotFoundException("Pair code not found"));

        pairCode.setUsed(true);
        pairCode.setUsedAt(LocalDateTime.now());
    }

    private PairCodeStatusResponse toStatusResponse(PairCode pairCode) {
        LocalDateTime now = LocalDateTime.now();
        return new PairCodeStatusResponse(
                pairCode.getCode(),
                Boolean.TRUE.equals(pairCode.getUsed()),
                !pairCode.getExpiresAt().isAfter(now),
                pairCode.getDeviceId(),
                pairCode.getExpiresAt(),
                pairCode.getUsedAt()
        );
    }
}
