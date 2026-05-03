package nifreebie.ardodo.config;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.util.DeviceHandshakeInterceptor;
import nifreebie.ardodo.util.DeviceWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DeviceHandshakeInterceptor interceptor;
    private final DeviceWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/devices")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}