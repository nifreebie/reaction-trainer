package nifreebie.ardodo.service.job;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.service.PairCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PairCodeGeneratorJob {

    private final PairCodeService service;

    @Value("${app.pair-code.min-count}")
    private int minCount;

    @Scheduled(fixedDelayString = "${app.pair-code.generate-delay-ms}")
    public void generate() {
        if (!service.hasPrepared(minCount)) {
            service.generateBatch();
        }
    }
}
