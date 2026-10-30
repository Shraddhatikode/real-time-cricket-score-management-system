package com.cricket.live.repo;

import com.cricket.live.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTeamOrderByIdAsc(String team);
    List<Player> findTop5ByOrderByRunsDesc();
    List<Player> findTop5ByOrderByWicketsDesc();
    @Query("select distinct p.team from Player p order by p.team")
    List<String> findTeams();
}
