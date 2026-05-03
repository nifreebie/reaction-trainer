package nifreebie.ardodo.dto.response;

public record RegisterDeviceResponse(
        String deviceId,
        String name,
        String firmwareVersion,
        String deviceToken
) {}