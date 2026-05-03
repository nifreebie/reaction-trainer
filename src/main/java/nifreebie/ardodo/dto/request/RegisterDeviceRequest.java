package nifreebie.ardodo.dto.request;

public record RegisterDeviceRequest(
        String deviceId,
        String name,
        String firmwareVersion
) {}