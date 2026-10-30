package com.cricket.live.service;

import com.cricket.live.dto.Dto.BallInput;
import com.cricket.live.model.BallEvent;
import com.cricket.live.model.Match;
import com.cricket.live.repo.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

/** Stands in for a real data feed: bowls one random ball in every live match each tick. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "simulation.enabled", havingValue = "true", matchIfMissing = true)
public class LiveSimulator {
    private static final int[] RUNS = {0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 4, 4, 6};
    private final MatchRepository matches;
    private final MatchService service;
    private final Random rnd = new Random();

    @Scheduled(fixedDelayString = "${simulation.tick-ms:2000}")
    public void tick() {
        for (Match m : matches.findByStatus(Match.Status.LIVE)) {
            try {
                service.recordBall(m.getId(), nextBall());
            } catch (RuntimeException ignored) { /* match finished concurrently */ }
        }
    }

    private BallInput nextBall() {
        int r = rnd.nextInt(100);
        if (r < 3) return new BallInput(0, BallEvent.Extra.WIDE, false);
        if (r < 5) return new BallInput(rnd.nextInt(2), BallEvent.Extra.NO_BALL, false);
        if (r < 11) return new BallInput(0, null, true);
        return new BallInput(RUNS[rnd.nextInt(RUNS.length)], null, false);
    }
}
