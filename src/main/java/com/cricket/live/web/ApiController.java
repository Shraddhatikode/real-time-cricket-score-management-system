package com.cricket.live.web;

import com.cricket.live.dto.Dto.*;
import com.cricket.live.model.BallEvent;
import com.cricket.live.model.Match;
import com.cricket.live.model.Player;
import com.cricket.live.repo.PlayerRepository;
import com.cricket.live.service.MatchService;
import com.cricket.live.service.SseBroadcaster;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final MatchService service;
    private final PlayerRepository players;
    private final SseBroadcaster broadcaster;

    // ---- matches
    @GetMapping("/matches")
    public List<MatchView> matches(@RequestParam(required = false) Match.Status status) { return service.list(status); }

    @GetMapping("/matches/{id}")
    public MatchView match(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/matches") @ResponseStatus(HttpStatus.CREATED)
    public MatchView create(@Valid @RequestBody CreateMatch req) { return service.create(req); }

    @PostMapping("/matches/{id}/start")
    public MatchView start(@PathVariable Long id) { return service.start(id); }

    /** Manual score update (what a scorer's app / data feed would call). */
    @PostMapping("/matches/{id}/ball")
    public MatchView ball(@PathVariable Long id, @Valid @RequestBody BallInput in) { return service.recordBall(id, in); }

    @GetMapping("/matches/{id}/commentary")
    public List<BallEvent> commentary(@PathVariable Long id, @RequestParam(defaultValue = "30") int limit) {
        return service.commentary(id, limit);
    }

    @GetMapping("/matches/{id}/summary")
    public MatchSummary summary(@PathVariable Long id) { return service.summary(id); }

    // ---- players & stats
    @GetMapping("/teams")
    public List<String> teams() { return players.findTeams(); }

    @GetMapping("/players")
    public List<Player> players(@RequestParam(required = false) String team) {
        return team == null ? players.findAll() : players.findByTeamOrderByIdAsc(team);
    }

    @GetMapping("/players/leaders")
    public Map<String, List<Player>> leaders() {
        return Map.of("topBatters", players.findTop5ByOrderByRunsDesc(),
                      "topBowlers", players.findTop5ByOrderByWicketsDesc());
    }

    // ---- real-time stream
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() { return broadcaster.register(); }
}
