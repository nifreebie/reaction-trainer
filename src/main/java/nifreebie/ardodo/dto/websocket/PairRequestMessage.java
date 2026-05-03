package nifreebie.ardodo.dto.websocket;

public record PairRequestMessage(
        String type,
        String code
) {}