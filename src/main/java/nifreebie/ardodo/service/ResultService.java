package nifreebie.ardodo.service;

import nifreebie.ardodo.dto.request.ResultCreateRequest;
import nifreebie.ardodo.dto.response.LeaderboardEntryResponse;
import nifreebie.ardodo.dto.response.ResultResponse;

import java.util.List;
import java.util.UUID;

public interface ResultService {
    UUID create(ResultCreateRequest request);

    List<LeaderboardEntryResponse> getLeaders(int limit);

    List<ResultResponse> getPlayerResults(UUID playerId, int limit);

    ResultResponse getBestResult(UUID playerId);
}
