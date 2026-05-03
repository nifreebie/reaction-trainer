package nifreebie.ardodo.service.job;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.service.PairCodeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PairCodeCleanupJob {

    private final PairCodeService pairCodeService;

    @Scheduled(fixedDelayString = "${app.pair-code.cleanup-delay-ms}")
    public void cleanup() {
        pairCodeService.cleanup();
    }
}
