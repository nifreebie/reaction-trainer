package nifreebie.ardodo.dto.response;

public record PlayerStatsResponse(
        long gamesCount,
        long resultsCount,
        Integer bestResultTimeMs,
        Integer bestReactionMs,
        Integer avgReactionMs,
        int hitsCount,
        int missesCount,
        int wrongButtonsCount,
        int falseStartsCount
) {
}
