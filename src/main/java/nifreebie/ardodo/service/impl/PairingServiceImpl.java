package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.GameProperties;
import nifreebie.ardodo.domain.*;
import nifreebie.ardodo.dto.websocket.PairingResult;
import nifreebie.ardodo.repository.DeviceRepository;
import nifreebie.ardodo.repository.GameSessionRepository;
import nifreebie.ardodo.repository.PairCodeRepository;
import nifreebie.ardodo.service.PairingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PairingServiceImpl implements PairingService {

    private final PairCodeRepository pairCodeRepository;
    private final DeviceRepository deviceRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GameProperties gameProperties;

    @Override
    @Transactional
    public PairingResult pairDevice(String deviceId, String code) {
        LocalDateTime now = LocalDateTime.now();

        PairCode pairCode = findActivePairCode(code, now);
        Device device = findOnlineDevice(deviceId);
        device.setPlayer(pairCode.getPlayer());

        GameSession session = createWaitingSession(pairCode, device, now);
        gameSessionRepository.save(session);

        markCodeUsed(pairCode, deviceId, now);

        return new PairingResult(
                session.getId(),
                pairCode.getPlayer().getName()
        );
    }

    private PairCode findActivePairCode(String code, LocalDateTime now) {
        return pairCodeRepository.findByCodeAndUsedFalseAndExpiresAtAfter(code, now)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));
    }

    private Device findOnlineDevice(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        if (!Boolean.TRUE.equals(device.getIsOnline())) {
            throw new IllegalArgumentException("Device is offline");
        }

        return device;
    }

    private GameSession createWaitingSession(PairCode pairCode, Device device, LocalDateTime now) {
        GameSession session = new GameSession();
        session.setPlayer(pairCode.getPlayer());
        session.setDevice(device);
        session.setStatus(SessionStatus.WAITING);
        session.setMode(GameMode.CLASSIC);
        session.setStartedAt(now);
        session.setRoundsCount(gameProperties.getRoundsCount());
        session.setTimeoutMs(gameProperties.getTimeoutMs());
        session.setFalseStartsCount(0);
        session.setWrongButtonsCount(0);
        session.setMissesCount(0);
        session.setHitsCount(0);
        session.setTotalReactionMs(0);
        session.setBestReactionMs(null);
        session.setAvgReactionMs(null);
        return session;
    }

    private void markCodeUsed(PairCode pairCode, String deviceId, LocalDateTime now) {
        pairCode.setUsed(true);
        pairCode.setUsedAt(now);
        pairCode.setDeviceId(deviceId);
    }
}
