package com.cricket.live.dto;

import com.cricket.live.model.BallEvent;
import jakarta.validation.constraints.*;
import java.util.List;

public final class Dto {
    private Dto() {}

    public record CreateMatch(@NotBlank String teamA, @NotBlank String teamB,
                              @NotBlank String venue, @Min(1) @Max(50) int overs) {}

    /** runs = runs scored off the bat (or extra runs on a wide). extra: WIDE / NO_BALL / null. */
    public record BallInput(@Min(0) @Max(7) int runs, BallEvent.Extra extra, boolean wicket) {}

    public record BatterLine(String name, int runs, int balls) {}
    public record BowlerLine(String name, String overs, int runs, int wickets) {}

    public record MatchView(Long id, String teamA, String teamB, String venue, String status,
                            int innings, String battingTeam, String score, String overs, int totalOvers,
                            Integer target, double runRate, Double requiredRate, String firstInnings,
                            String result, BatterLine striker, BatterLine nonStriker, BowlerLine bowler) {}

    /** Payload pushed to dashboards over SSE after every ball. */
    public record ScoreUpdate(MatchView match, BallEvent ball) {}

    public record MatchSummary(Long matchId, String status, String result, String innings1, String innings2,
                               int fours, int sixes, int extras, String topBatter, int topBatterRuns,
                               List<String> fallOfWickets) {}
}
