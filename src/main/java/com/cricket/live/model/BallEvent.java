package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

/** One delivery (ball-by-ball log). Source of truth for commentary and summaries. */
@Entity
@Data
public class BallEvent {
    public enum Extra { WIDE, NO_BALL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long matchId;
    private int innings, overNo, ballNo, batRuns, totalRuns;
    private String batsman, bowler, scoreAfter, commentary;
    @Enumerated(EnumType.STRING)
    private Extra extra;
    private boolean wicket;
    private Instant createdAt = Instant.now();
}
