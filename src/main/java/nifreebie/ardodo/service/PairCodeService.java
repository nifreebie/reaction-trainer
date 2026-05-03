package nifreebie.ardodo.service;

import nifreebie.ardodo.dto.response.PairCodeResponse;
import nifreebie.ardodo.dto.response.PairCodeStatusResponse;

import java.util.Optional;
import java.util.UUID;

public interface PairCodeService {
    PairCodeResponse generateCode(UUID playerId);

    void generateBatch();

    void cleanup();

    boolean hasPrepared(int minCount);

    Optional<PairCodeStatusResponse> getActive(UUID playerId);

    PairCodeStatusResponse getStatus(UUID playerId, String code);

    void cancel(UUID playerId, String code);
}
