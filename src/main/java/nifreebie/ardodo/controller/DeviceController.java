package nifreebie.ardodo.controller;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.PlayerDetails;
import nifreebie.ardodo.dto.request.RegisterDeviceRequest;
import nifreebie.ardodo.dto.request.UpdateDeviceRequest;
import nifreebie.ardodo.dto.response.DeviceResponse;
import nifreebie.ardodo.dto.response.RegisterDeviceResponse;
import nifreebie.ardodo.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    public RegisterDeviceResponse register(@RequestBody RegisterDeviceRequest request) {
        return deviceService.registerDevice(request);
    }

    @GetMapping("/me")
    public List<DeviceResponse> mine(@AuthenticationPrincipal PlayerDetails principal) {
        return deviceService.getPlayerDevices(principal.getId());
    }

    @GetMapping("/online")
    public List<DeviceResponse> online() {
        return deviceService.getOnlineDevices();
    }

    @GetMapping("/{id}")
    public DeviceResponse get(
            @AuthenticationPrincipal PlayerDetails principal,
            @PathVariable String id
    ) {
        return deviceService.getPlayerDevice(principal.getId(), id);
    }

    @GetMapping("/{id}/status")
    public DeviceResponse status(
            @AuthenticationPrincipal PlayerDetails principal,
            @PathVariable String id
    ) {
        return deviceService.getDeviceStatus(principal.getId(), id);
    }

    @PatchMapping("/{id}")
    public DeviceResponse update(
            @AuthenticationPrincipal PlayerDetails principal,
            @PathVariable String id,
            @RequestBody UpdateDeviceRequest request
    ) {
        return deviceService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal PlayerDetails principal,
            @PathVariable String id
    ) {
        deviceService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
