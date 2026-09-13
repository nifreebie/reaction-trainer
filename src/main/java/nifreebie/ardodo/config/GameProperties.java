package nifreebie.ardodo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.game")
public class GameProperties {

    private int roundsCount = 10;
    private int timeoutMs = 30000;
    private int numberMin = -99;
    private int numberMax = 99;

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

    public int getNumberMin() {
        return numberMin;
    }

    public void setNumberMin(int numberMin) {
        this.numberMin = numberMin;
    }

    public int getNumberMax() {
        return numberMax;
    }

    public void setNumberMax(int numberMax) {
        this.numberMax = numberMax;
    }

    public int randomNumberRange() {
        return numberMax - numberMin + 1;
    }
}
