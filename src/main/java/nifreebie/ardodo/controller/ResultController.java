package nifreebie.ardodo.controller;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.PlayerDetails;
import nifreebie.ardodo.dto.request.ResultCreateRequest;
import nifreebie.ardodo.dto.response.LeaderboardEntryResponse;
import nifreebie.ardodo.dto.response.ResultCreateResponse;
import nifreebie.ardodo.dto.response.ResultResponse;
import nifreebie.ardodo.service.ResultService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @PostMapping
    public ResultCreateResponse create(@RequestBody ResultCreateRequest request) {
        return new ResultCreateResponse(resultService.create(request));
    }

    @GetMapping("/leaders")
    public List<LeaderboardEntryResponse> leaders(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return resultService.getLeaders(limit);
    }

    @GetMapping("/me")
    public List<ResultResponse> mine(
            @AuthenticationPrincipal PlayerDetails principal,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return resultService.getPlayerResults(principal.getId(), limit);
    }

    @GetMapping("/me/best")
    public ResultResponse best(@AuthenticationPrincipal PlayerDetails principal) {
        return resultService.getBestResult(principal.getId());
    }
}
