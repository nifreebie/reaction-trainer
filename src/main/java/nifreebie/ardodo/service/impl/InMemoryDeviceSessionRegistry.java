package nifreebie.ardodo.service.impl;

import nifreebie.ardodo.service.DeviceSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryDeviceSessionRegistry implements DeviceSessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void register(String deviceId, WebSocketSession session) {
        sessions.put(deviceId, session);
    }

    @Override
    public void unregister(String deviceId) {
        sessions.remove(deviceId);
    }

    @Override
    public WebSocketSession get(String deviceId) {
        return sessions.get(deviceId);
    }
}
