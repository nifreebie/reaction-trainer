package nifreebie.ardodo.controller;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.PlayerDetails;
import nifreebie.ardodo.dto.response.PairCodeResponse;
import nifreebie.ardodo.dto.response.PairCodeStatusResponse;
import nifreebie.ardodo.service.PairCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pair-codes")
@RequiredArgsConstructor
public class PairCodeController {

    private final PairCodeService pairCodeService;

    @PostMapping
    public PairCodeResponse generate(@AuthenticationPrincipal PlayerDetails principal) {
        return pairCodeService.generateCode(principal.getId());
    }

    @GetMapping("/active")
    public ResponseEntity<PairCodeStatusResponse> active(@AuthenticationPrincipal PlayerDetails principal) {
        return ResponseEntity.of(pairCodeService.getActive(principal.getId()));
    }

    @GetMapping("/{code}/status")
    public PairCodeStatusResponse status(
            @AuthenticationPrincipal PlayerDetails principal,
            @PathVariable String code
    ) {
        return pairCodeService.getStatus(principal.getId(), code);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal PlayerDetails principal,
            @PathVariable String code
    ) {
        pairCodeService.cancel(principal.getId(), code);
        return ResponseEntity.noContent().build();
    }
}
