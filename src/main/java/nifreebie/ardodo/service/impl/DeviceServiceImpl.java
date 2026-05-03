package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.domain.Device;
import nifreebie.ardodo.dto.request.RegisterDeviceRequest;
import nifreebie.ardodo.dto.request.UpdateDeviceRequest;
import nifreebie.ardodo.dto.response.DeviceResponse;
import nifreebie.ardodo.dto.response.RegisterDeviceResponse;
import nifreebie.ardodo.repository.DeviceRepository;
import nifreebie.ardodo.service.DeviceService;
import nifreebie.ardodo.util.DuplicateUsernameException;
import nifreebie.ardodo.util.BadRequestException;
import nifreebie.ardodo.util.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int TOKEN_BYTES = 32;

    private final DeviceRepository deviceRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public RegisterDeviceResponse registerDevice(RegisterDeviceRequest request) {
        if (deviceRepository.existsById(request.deviceId())) {
            throw new DuplicateUsernameException("Device already exists");
        }

        String token = generateToken();
        String tokenHash = hashToken(token);

        Device device = Device.builder()
                .id(request.deviceId())
                .name(request.name())
                .firmwareVersion(request.firmwareVersion())
                .deviceToken(tokenHash)
                .isOnline(false)
                .lastSeenAt(LocalDateTime.now())
                .build();

        deviceRepository.save(device);

        return new RegisterDeviceResponse(
                device.getId(),
                device.getName(),
                device.getFirmwareVersion(),
                token
        );
    }

    @Override
    public boolean validate(String deviceId, String token) {
        return deviceRepository.findById(deviceId)
                .map(device -> hashToken(token).equals(device.getDeviceToken()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> getPlayerDevices(UUID playerId) {
        return deviceRepository.findByPlayerId(playerId)
                .stream()
                .sorted(Comparator.comparing(Device::getLastSeenAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> getOnlineDevices() {
        return deviceRepository.findByIsOnlineTrue()
                .stream()
                .sorted(Comparator.comparing(Device::getLastSeenAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponse getPlayerDevice(UUID playerId, String deviceId) {
        return toResponse(findPlayerDevice(playerId, deviceId));
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponse getDeviceStatus(UUID playerId, String deviceId) {
        return getPlayerDevice(playerId, deviceId);
    }

    @Override
    @Transactional
    public DeviceResponse update(UUID playerId, String deviceId, UpdateDeviceRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("name is required");
        }

        Device device = findPlayerDevice(playerId, deviceId);
        device.setName(request.name().trim());
        return toResponse(device);
    }

    @Override
    @Transactional
    public void delete(UUID playerId, String deviceId) {
        Device device = findPlayerDevice(playerId, deviceId);
        device.setPlayer(null);
    }

    private Device findPlayerDevice(UUID playerId, String deviceId) {
        return deviceRepository.findByIdAndPlayerId(deviceId, playerId)
                .orElseThrow(() -> new NotFoundException("Device not found"));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash device token", e);
        }
    }

    private DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getFirmwareVersion(),
                device.getIsOnline(),
                device.getLastSeenAt(),
                device.getRegisteredAt()
        );
    }
}
