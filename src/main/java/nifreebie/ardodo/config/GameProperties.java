package nifreebie.ardodo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.game")
public class GameProperties {

    private int roundsCount = 10;
    private int timeoutMs = 1500;
    private int targetButtonsCount = 8;
    private int stimulusDelayMinMs = 500;
    private int stimulusDelayMaxMs = 2000;

    public int getRoundsCount() {
        return roundsCount;
    }

    public void setRoundsCount(int roundsCount) {
        this.roundsCount = roundsCount;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getTargetButtonsCount() {
        return targetButtonsCount;
    }

    public void setTargetButtonsCount(int targetButtonsCount) {
        this.targetButtonsCount = targetButtonsCount;
    }

    public int getStimulusDelayMinMs() {
        return stimulusDelayMinMs;
    }

    public void setStimulusDelayMinMs(int stimulusDelayMinMs) {
        this.stimulusDelayMinMs = stimulusDelayMinMs;
    }

    public int getStimulusDelayMaxMs() {
        return stimulusDelayMaxMs;
    }

    public void setStimulusDelayMaxMs(int stimulusDelayMaxMs) {
        this.stimulusDelayMaxMs = stimulusDelayMaxMs;
    }

    public int randomStimulusDelayRange() {
        return stimulusDelayMaxMs - stimulusDelayMinMs + 1;
    }
}
