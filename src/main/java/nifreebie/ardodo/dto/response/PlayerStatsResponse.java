package nifreebie.ardodo.dto.response;

public record PlayerStatsResponse(
        long gamesCount,
        long resultsCount,
        Integer bestResultTimeMs,
        Integer bestAnswerTimeMs,
        Integer avgAnswerTimeMs,
        int correctAnswersCount,
        int incorrectAnswersCount,
        int missedAnswersCount
) {
}
