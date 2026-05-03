package nifreebie.ardodo.dto.websocket;

public record ErrorMessage(
        String type,
        String reason
) {}