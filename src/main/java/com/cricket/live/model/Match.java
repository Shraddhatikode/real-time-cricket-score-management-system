package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "cricket_match")
public class Match {
    public enum Status { UPCOMING, LIVE, COMPLETED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String teamA, teamB, venue;
    private int totalOvers;
    @Enumerated(EnumType.STRING)
    private Status status = Status.UPCOMING;

    // current innings state
    private int innings = 1;
    private String battingTeam, bowlingTeam;
    private int runs, wickets, balls;           // balls = legal deliveries
    private Long strikerId, nonStrikerId, bowlerId;
    private int nextBatterIdx;

    // first innings snapshot + chase info
    private int firstInningsRuns, firstInningsWickets, firstInningsBalls;
    private Integer target;
    private String result;
}
