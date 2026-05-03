package nifreebie.ardodo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nifreebie.ardodo.domain.RoundResult;
import nifreebie.ardodo.dto.websocket.PairingResult;
import nifreebie.ardodo.repository.DeviceRepository;
import nifreebie.ardodo.service.DeviceSessionRegistry;
import nifreebie.ardodo.service.GameFlowService;
import nifreebie.ardodo.service.PairingService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final DeviceRepository deviceRepository;
    private final PairingService pairingService;
    private final GameFlowService gameFlowService;
    private final DeviceSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String deviceId = (String) session.getAttributes().get("deviceId");

        if (deviceId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("No deviceId"));
            return;
        }

        sessionRegistry.register(deviceId, session);
        log.info("Device websocket connected: {}", deviceId);

        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setIsOnline(true);
            device.setLastSeenAt(LocalDateTime.now());
            deviceRepository.save(device);
        });

        send(session, Map.of(
                "type", "connected",
                "deviceId", deviceId
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String deviceId = (String) session.getAttributes().get("deviceId");

        if (deviceId != null) {
            log.info("Device websocket disconnected: {}, status={}", deviceId, status);
            sessionRegistry.unregister(deviceId);

            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.setIsOnline(false);
                deviceRepository.save(device);
            });
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText("");

            switch (type) {
                case "heartbeat" -> handleHeartbeat(session);
                case "pair_request" -> handlePairRequest(session, node);
                case "round_result" -> handleRoundResult(session, node);
                default -> sendError(session, "UNKNOWN_TYPE");
            }
        } catch (Exception e) {
            sendError(session, "INVALID_JSON");
        }
    }

    private void handleHeartbeat(WebSocketSession session) {
        String deviceId = (String) session.getAttributes().get("deviceId");

        if (deviceId == null) {
            sendError(session, "INVALID_REQUEST");
            return;
        }

        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setIsOnline(true);
            device.setLastSeenAt(LocalDateTime.now());
            deviceRepository.save(device);
        });
    }

    private void handlePairRequest(WebSocketSession session, JsonNode node) {
        try {
            String deviceId = (String) session.getAttributes().get("deviceId");
            String code = node.path("code").asText(null);
            log.info("Pair request from device={}", deviceId);

            if (deviceId == null || code == null || code.isBlank()) {
                sendError(session, "INVALID_REQUEST");
                return;
            }

            PairingResult result = pairingService.pairDevice(deviceId, code);

            send(session, Map.of(
                    "type", "pair_success",
                    "sessionId", result.sessionId().toString(),
                    "playerName", result.playerName()
            ));
            log.info("Pair success for device={}, sessionId={}", deviceId, result.sessionId());

            // Start the game immediately after successful pairing.
            gameFlowService.startGame(result.sessionId());

        } catch (IllegalArgumentException | IllegalStateException ex) {
            sendError(session, ex.getMessage());
        } catch (Exception ex) {
            sendError(session, "PAIR_FAILED");
        }
    }

    private void handleRoundResult(WebSocketSession session, JsonNode node) {
        try {
            String deviceId = (String) session.getAttributes().get("deviceId");
            String sessionIdRaw = node.path("sessionId").asText(null);
            String resultRaw = node.path("result").asText(null);

            if (deviceId == null || sessionIdRaw == null || resultRaw == null) {
                sendError(session, "INVALID_REQUEST");
                return;
            }

            UUID sessionId = UUID.fromString(sessionIdRaw);
            int roundNumber = node.path("roundNumber").asInt();

            if (roundNumber <= 0) {
                sendError(session, "INVALID_ROUND_NUMBER");
                return;
            }

            Integer pressedButton = node.hasNonNull("pressedButton")
                    ? node.get("pressedButton").asInt()
                    : null;

            Integer reactionTimeMs = node.hasNonNull("reactionTimeMs")
                    ? node.get("reactionTimeMs").asInt()
                    : null;

            RoundResult result = RoundResult.valueOf(resultRaw.toUpperCase());

            gameFlowService.handleRoundResult(
                    deviceId,
                    sessionId,
                    roundNumber,
                    pressedButton,
                    reactionTimeMs,
                    result
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(session, e.getMessage());
        } catch (Exception e) {
            sendError(session, "ROUND_RESULT_FAILED");
        }
    }

    private void send(WebSocketSession session, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.warn("Failed to send websocket message", e);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        send(session, Map.of(
                "type", "error",
                "message", message
        ));
    }
}
