package nifreebie.ardodo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nifreebie.ardodo.service.DeviceMessageSender;
import nifreebie.ardodo.service.DeviceSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketDeviceMessageSender implements DeviceMessageSender {

    private final DeviceSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void sendToDevice(String deviceId, Object payload) {
        WebSocketSession session = sessionRegistry.get(deviceId);

        if (session == null || !session.isOpen()) {
            log.warn("Cannot send to device={}: websocket is not open", deviceId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            log.debug("WS -> device={}: {}", deviceId, json);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            throw new RuntimeException("Failed to send websocket message", e);
        }
    }
}
