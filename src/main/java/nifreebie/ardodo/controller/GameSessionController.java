package nifreebie.ardodo.controller;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.PlayerDetails;
import nifreebie.ardodo.dto.response.RoundResponse;
import nifreebie.ardodo.dto.response.SessionResponse;
import nifreebie.ardodo.service.GameSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameSessionService gameSessionService;

    @GetMapping("/current")
    public ResponseEntity<SessionResponse> current(@AuthenticationPrincipal PlayerDetails principal) {
        return ResponseEntity.of(gameSessionService.getCurrent(principal.getId()));
    }

    @GetMapping("/me")
    public List<SessionResponse> mine(
            @AuthenticationPrincipal PlayerDetails principal,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return gameSessionService.getPlayerSessions(principal.getId(), limit);
    }

    @GetMapping("/{id}")
    public SessionResponse get(
            @AuthenticationPrincipal PlayerDetails principal,
            @PathVariable UUID id
    ) {
        return gameSessionService.getSession(principal.getId(), id);
    }

    @GetMapping("/{id}/rounds")
    public List<RoundResponse> rounds(
            @AuthenticationPrincipal PlayerDetails principal,
            @PathVariable UUID id
    ) {
        return gameSessionService.getSessionRounds(principal.getId(), id);
    }
}
