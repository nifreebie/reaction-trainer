package nifreebie.ardodo.service;

import nifreebie.ardodo.dto.request.RegisterDeviceRequest;
import nifreebie.ardodo.dto.request.UpdateDeviceRequest;
import nifreebie.ardodo.dto.response.DeviceResponse;
import nifreebie.ardodo.dto.response.RegisterDeviceResponse;

import java.util.List;
import java.util.UUID;

public interface DeviceService {
    RegisterDeviceResponse registerDevice(RegisterDeviceRequest request);
    boolean validate(String deviceId, String token);

    List<DeviceResponse> getPlayerDevices(UUID playerId);

    List<DeviceResponse> getOnlineDevices();

    DeviceResponse getPlayerDevice(UUID playerId, String deviceId);

    DeviceResponse getDeviceStatus(UUID playerId, String deviceId);

    DeviceResponse update(UUID playerId, String deviceId, UpdateDeviceRequest request);

    void delete(UUID playerId, String deviceId);
}
