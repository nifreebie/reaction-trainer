package nifreebie.ardodo.service;

import nifreebie.ardodo.dto.PlayerDTO;
import nifreebie.ardodo.dto.request.UpdatePlayerRequest;
import nifreebie.ardodo.dto.response.PlayerStatsResponse;

import java.util.UUID;

public interface PlayerService {
    PlayerDTO getUserByName(String name);

    PlayerDTO getUserById(UUID id);

    boolean isExistsById(UUID id);

    boolean isExistsByName(String name);

    PlayerDTO update(UUID id, UpdatePlayerRequest request);

    PlayerStatsResponse getStats(UUID id);
}
