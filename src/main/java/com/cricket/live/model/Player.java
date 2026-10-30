package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Data;

/** Cumulative (tournament) statistics for a player. */
@Entity
@Data
public class Player {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name, team, role;
    private int runs, ballsFaced, fours, sixes;
    private int wickets, runsConceded, ballsBowled;

    public double getStrikeRate() {
        return ballsFaced == 0 ? 0 : Math.round(runs * 10000.0 / ballsFaced) / 100.0;
    }
    public double getEconomy() {
        return ballsBowled == 0 ? 0 : Math.round(runsConceded * 600.0 / ballsBowled) / 100.0;
    }
}
