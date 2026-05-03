package nifreebie.ardodo.service;

public interface DeviceMessageSender {
    void sendToDevice(String deviceId, Object payload);
}