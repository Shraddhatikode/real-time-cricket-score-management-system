package com.cricket.live.config;

import com.cricket.live.dto.Dto.CreateMatch;
import com.cricket.live.model.Player;
import com.cricket.live.repo.PlayerRepository;
import com.cricket.live.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private static final String[] TEAMS = {"Mumbai Mavericks", "Delhi Dynamos", "Chennai Chargers", "Bengaluru Blazers"};
    private static final String[] FIRST = {"Aarav", "Vihaan", "Rohan", "Kabir", "Ishaan", "Arjun", "Dev", "Rahul", "Siddharth", "Karthik", "Neil"};
    private static final String[] LAST = {"Sharma", "Verma", "Iyer", "Nair", "Patil", "Reddy", "Khan", "Gupta", "Joshi", "Menon", "Rao"};
    // positions 6..10 are the bowling attack
    private static final String[] ROLES = {"BATTER", "BATTER", "BATTER", "BATTER", "WICKET_KEEPER",
            "ALL_ROUNDER", "ALL_ROUNDER", "BOWLER", "BOWLER", "BOWLER", "BOWLER"};

    private final PlayerRepository players;
    private final MatchService matches;

    @Override
    public void run(String... args) {
        if (players.count() > 0) return;
        for (int t = 0; t < TEAMS.length; t++) {
            for (int i = 0; i < 11; i++) {
                Player p = new Player();
                p.setName(FIRST[i] + " " + LAST[(i + t * 3) % 11]);
                p.setTeam(TEAMS[t]);
                p.setRole(ROLES[i]);
                players.save(p);
            }
        }
        matches.start(matches.create(new CreateMatch(TEAMS[0], TEAMS[1], "Wankhede Stadium", 20)).id());
        matches.start(matches.create(new CreateMatch(TEAMS[2], TEAMS[3], "Chepauk Stadium", 5)).id());
        matches.create(new CreateMatch(TEAMS[0], TEAMS[2], "Eden Gardens", 10));
    }
}
