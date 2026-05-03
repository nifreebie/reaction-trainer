package nifreebie.ardodo.controller;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.PlayerDetails;
import nifreebie.ardodo.dto.PlayerDTO;
import nifreebie.ardodo.dto.request.UpdatePlayerRequest;
import nifreebie.ardodo.dto.response.PlayerStatsResponse;
import nifreebie.ardodo.service.PlayerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public PlayerDTO current(@AuthenticationPrincipal PlayerDetails principal) {
        return playerService.getUserById(principal.getId());
    }

    @PatchMapping
    public PlayerDTO update(
            @AuthenticationPrincipal PlayerDetails principal,
            @RequestBody UpdatePlayerRequest request
    ) {
        return playerService.update(principal.getId(), request);
    }

    @GetMapping("/stats")
    public PlayerStatsResponse stats(@AuthenticationPrincipal PlayerDetails principal) {
        return playerService.getStats(principal.getId());
    }
}
