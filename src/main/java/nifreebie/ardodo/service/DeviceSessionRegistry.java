package nifreebie.ardodo.service;

import org.springframework.web.socket.WebSocketSession;

public interface DeviceSessionRegistry {
    void register(String deviceId, WebSocketSession session);

    void unregister(String deviceId);

    WebSocketSession get(String deviceId);
}
