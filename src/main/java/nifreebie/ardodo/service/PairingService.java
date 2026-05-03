package nifreebie.ardodo.service;


import nifreebie.ardodo.dto.websocket.PairingResult;

public interface PairingService {
    PairingResult pairDevice(String deviceId, String code);
}