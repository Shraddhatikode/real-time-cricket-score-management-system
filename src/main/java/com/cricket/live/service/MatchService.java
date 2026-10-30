package com.cricket.live.service;

import com.cricket.live.dto.Dto.*;
import com.cricket.live.model.*;
import com.cricket.live.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matches;
    private final PlayerRepository players;
    private final BallEventRepository events;
    private final SseBroadcaster broadcaster;
    /** Per-match lock: balls of one match are strictly ordered, different matches run in parallel. */
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    private Object lock(Long id) { return locks.computeIfAbsent(id, k -> new Object()); }

    // ---------- queries ----------
    public Match find(Long id) {
        return matches.findById(id).orElseThrow(() -> new NoSuchElementException("Match " + id + " not found"));
    }
    public List<MatchView> list(Match.Status status) {
        List<Match> all = status == null ? matches.findAll() : matches.findByStatus(status);
        return all.stream().map(this::view).toList();
    }
    public MatchView get(Long id) { return view(find(id)); }

    public List<BallEvent> commentary(Long id, int limit) {
        find(id);
        return events.findByMatchIdOrderByIdDesc(id, PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
    }

    // ---------- commands ----------
    public MatchView create(CreateMatch req) {
        if (req.teamA().equalsIgnoreCase(req.teamB())) throw new IllegalArgumentException("Teams must be different");
        for (String t : List.of(req.teamA(), req.teamB()))
            if (players.findByTeamOrderByIdAsc(t).size() < 11)
                throw new IllegalArgumentException("Team '" + t + "' needs at least 11 players");
        Match m = new Match();
        m.setTeamA(req.teamA()); m.setTeamB(req.teamB()); m.setVenue(req.venue());
        m.setTotalOvers(req.overs());
        m.setBattingTeam(req.teamA()); m.setBowlingTeam(req.teamB());
        return view(matches.save(m));
    }

    public MatchView start(Long id) {
        synchronized (lock(id)) {
            Match m = find(id);
            if (m.getStatus() != Match.Status.UPCOMING) throw new IllegalStateException("Match already started");
            m.setStatus(Match.Status.LIVE);
            initInnings(m);
            matches.save(m);
            return publish(m, null);
        }
    }

    public MatchView recordBall(Long id, BallInput in) {
        synchronized (lock(id)) {
            Match m = find(id);
            if (m.getStatus() != Match.Status.LIVE) throw new IllegalStateException("Match is not live");

            BallEvent.Extra extra = in.extra();
            int runsIn = in.runs();
            int bat, total;
            if (extra == BallEvent.Extra.WIDE)         { bat = 0;      total = 1 + runsIn; }
            else if (extra == BallEvent.Extra.NO_BALL) { bat = runsIn; total = 1 + runsIn; }
            else                                       { bat = runsIn; total = runsIn; }
            boolean legal = extra == null;
            boolean wicket = in.wicket() && legal;

            Player striker = players.findById(m.getStrikerId()).orElseThrow();
            Player bowler = players.findById(m.getBowlerId()).orElseThrow();
            int overNo = m.getBalls() / 6, ballNo = m.getBalls() % 6 + 1;

            // --- apply to match + player stats
            m.setRuns(m.getRuns() + total);
            bowler.setRunsConceded(bowler.getRunsConceded() + total);
            if (extra != BallEvent.Extra.WIDE) {
                striker.setBallsFaced(striker.getBallsFaced() + 1);
                striker.setRuns(striker.getRuns() + bat);
                if (bat == 4) striker.setFours(striker.getFours() + 1);
                if (bat == 6) striker.setSixes(striker.getSixes() + 1);
            }
            if (legal) {
                m.setBalls(m.getBalls() + 1);
                bowler.setBallsBowled(bowler.getBallsBowled() + 1);
            }
            if (wicket) {
                m.setWickets(m.getWickets() + 1);
                bowler.setWickets(bowler.getWickets() + 1);
            }
            players.save(striker);
            players.save(bowler);

            // --- batter replacement / strike rotation
            if (wicket) {
                List<Player> order = players.findByTeamOrderByIdAsc(m.getBattingTeam());
                if (m.getWickets() < 10 && m.getNextBatterIdx() < order.size()) {
                    m.setStrikerId(order.get(m.getNextBatterIdx()).getId());
                    m.setNextBatterIdx(m.getNextBatterIdx() + 1);
                }
            } else if (bat % 2 == 1) {
                swapStrike(m);
            }
            if (legal && m.getBalls() % 6 == 0) {          // end of over
                swapStrike(m);
                m.setBowlerId(pickBowler(m, m.getBalls() / 6));
            }

            // --- ball log
            BallEvent ev = new BallEvent();
            ev.setMatchId(id); ev.setInnings(m.getInnings());
            ev.setOverNo(overNo); ev.setBallNo(ballNo);
            ev.setBatsman(striker.getName()); ev.setBowler(bowler.getName());
            ev.setBatRuns(bat); ev.setTotalRuns(total); ev.setExtra(extra); ev.setWicket(wicket);
            ev.setScoreAfter(m.getRuns() + "/" + m.getWickets());
            ev.setCommentary(describe(striker, bowler, extra, bat, runsIn, wicket));
            events.save(ev);

            boolean inningsOver = m.getWickets() >= 10
                    || m.getBalls() >= m.getTotalOvers() * 6
                    || (m.getInnings() == 2 && m.getRuns() >= m.getTarget());
            if (inningsOver) endInnings(m);
            matches.save(m);
            return publish(m, ev);
        }
    }

    // ---------- summary ----------
    public MatchSummary summary(Long id) {
        Match m = find(id);
        List<BallEvent> all = events.findByMatchIdOrderByIdAsc(id);
        int fours = (int) all.stream().filter(e -> e.getBatRuns() == 4).count();
        int sixes = (int) all.stream().filter(e -> e.getBatRuns() == 6).count();
        int extras = all.stream().filter(e -> e.getExtra() != null)
                .mapToInt(e -> e.getExtra() == BallEvent.Extra.WIDE ? e.getTotalRuns() : 1).sum();
        Map<String, Integer> byBat = all.stream().collect(
                Collectors.groupingBy(BallEvent::getBatsman, Collectors.summingInt(BallEvent::getBatRuns)));
        Map.Entry<String, Integer> top = byBat.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        List<String> fow = all.stream().filter(BallEvent::isWicket)
                .map(e -> e.getBatsman() + " b " + e.getBowler() + "  " + e.getScoreAfter()
                        + " (" + e.getOverNo() + "." + e.getBallNo() + ")").toList();

        String i1 = null, i2 = null;
        if (m.getStatus() == Match.Status.LIVE && m.getInnings() == 1) {
            i1 = inningsLine(m.getBattingTeam(), m.getRuns(), m.getWickets(), m.getBalls());
        } else if (m.getStatus() != Match.Status.UPCOMING) {
            i1 = inningsLine(m.getBowlingTeam(), m.getFirstInningsRuns(), m.getFirstInningsWickets(), m.getFirstInningsBalls());
            i2 = inningsLine(m.getBattingTeam(), m.getRuns(), m.getWickets(), m.getBalls());
        }
        return new MatchSummary(id, m.getStatus().name(), m.getResult(), i1, i2, fours, sixes, extras,
                top == null ? null : top.getKey(), top == null ? 0 : top.getValue(), fow);
    }

    // ---------- view ----------
    public MatchView view(Match m) {
        boolean live = m.getStatus() == Match.Status.LIVE;
        List<BallEvent> evs = m.getStatus() == Match.Status.UPCOMING
                ? List.of() : events.findByMatchIdAndInnings(m.getId(), m.getInnings());
        int balls = m.getBalls();
        double rr = balls == 0 ? 0 : round2(m.getRuns() * 6.0 / balls);
        Double req = null;
        if (live && m.getInnings() == 2 && balls < m.getTotalOvers() * 6)
            req = round2((m.getTarget() - m.getRuns()) * 6.0 / (m.getTotalOvers() * 6 - balls));
        String first = (m.getInnings() == 2 && m.getStatus() != Match.Status.UPCOMING)
                ? inningsLine(m.getBowlingTeam(), m.getFirstInningsRuns(), m.getFirstInningsWickets(), m.getFirstInningsBalls())
                : null;
        return new MatchView(m.getId(), m.getTeamA(), m.getTeamB(), m.getVenue(), m.getStatus().name(),
                m.getInnings(), m.getBattingTeam(), m.getRuns() + "/" + m.getWickets(), overs(balls),
                m.getTotalOvers(), m.getTarget(), rr, req, first, m.getResult(),
                live ? batter(m.getStrikerId(), evs) : null,
                live ? batter(m.getNonStrikerId(), evs) : null,
                live ? bowler(m.getBowlerId(), evs) : null);
    }

    // ---------- helpers ----------
    private MatchView publish(Match m, BallEvent ev) {
        MatchView v = view(m);
        broadcaster.broadcast("score", new ScoreUpdate(v, ev));
        return v;
    }

    private void initInnings(Match m) {
        List<Player> bat = players.findByTeamOrderByIdAsc(m.getBattingTeam());
        m.setRuns(0); m.setWickets(0); m.setBalls(0);
        m.setStrikerId(bat.get(0).getId());
        m.setNonStrikerId(bat.get(1).getId());
        m.setNextBatterIdx(2);
        m.setBowlerId(pickBowler(m, 0));
    }

    /** Players 6..10 of the bowling side (all-rounder + 4 bowlers) rotate, never bowling consecutive overs. */
    private Long pickBowler(Match m, int overIdx) {
        List<Player> bowl = players.findByTeamOrderByIdAsc(m.getBowlingTeam());
        return bowl.get(6 + overIdx % 5).getId();
    }

    private void swapStrike(Match m) {
        Long t = m.getStrikerId();
        m.setStrikerId(m.getNonStrikerId());
        m.setNonStrikerId(t);
    }

    private void endInnings(Match m) {
        if (m.getInnings() == 1) {
            m.setFirstInningsRuns(m.getRuns());
            m.setFirstInningsWickets(m.getWickets());
            m.setFirstInningsBalls(m.getBalls());
            m.setTarget(m.getRuns() + 1);
            m.setInnings(2);
            String t = m.getBattingTeam();
            m.setBattingTeam(m.getBowlingTeam());
            m.setBowlingTeam(t);
            initInnings(m);
        } else {
            m.setStatus(Match.Status.COMPLETED);
            if (m.getRuns() >= m.getTarget())
                m.setResult(m.getBattingTeam() + " won by " + (10 - m.getWickets()) + " wickets");
            else if (m.getRuns() == m.getTarget() - 1)
                m.setResult("Match tied");
            else
                m.setResult(m.getBowlingTeam() + " won by " + (m.getTarget() - 1 - m.getRuns()) + " runs");
        }
    }

    private String describe(Player bat, Player bowl, BallEvent.Extra extra, int batRuns, int runsIn, boolean wicket) {
        if (wicket) return "OUT! " + bat.getName() + " dismissed by " + bowl.getName();
        if (extra == BallEvent.Extra.WIDE) return "Wide" + (runsIn > 0 ? " +" + runsIn : "") + " from " + bowl.getName();
        if (extra == BallEvent.Extra.NO_BALL) return "No ball! " + batRuns + " off the bat";
        if (batRuns == 6) return "SIX! " + bat.getName() + " launches " + bowl.getName() + " over the rope";
        if (batRuns == 4) return "FOUR! " + bat.getName() + " finds the gap";
        if (batRuns == 0) return "Dot ball";
        return bat.getName() + " takes " + batRuns + (batRuns == 1 ? " run" : " runs");
    }

    private BatterLine batter(Long id, List<BallEvent> evs) {
        Player p = players.findById(id).orElse(null);
        if (p == null) return null;
        String name = p.getName();
        int runs = evs.stream().filter(e -> name.equals(e.getBatsman())).mapToInt(BallEvent::getBatRuns).sum();
        int balls = (int) evs.stream().filter(e -> name.equals(e.getBatsman()) && e.getExtra() != BallEvent.Extra.WIDE).count();
        return new BatterLine(name, runs, balls);
    }

    private BowlerLine bowler(Long id, List<BallEvent> evs) {
        Player p = players.findById(id).orElse(null);
        if (p == null) return null;
        String name = p.getName();
        List<BallEvent> mine = evs.stream().filter(e -> name.equals(e.getBowler())).toList();
        int legal = (int) mine.stream().filter(e -> e.getExtra() == null).count();
        return new BowlerLine(name, overs(legal), mine.stream().mapToInt(BallEvent::getTotalRuns).sum(),
                (int) mine.stream().filter(BallEvent::isWicket).count());
    }

    private static String overs(int balls) { return (balls / 6) + "." + (balls % 6); }
    private static double round2(double v) { return Math.round(v * 100) / 100.0; }
    private static String inningsLine(String team, int r, int w, int b) {
        return team + " " + r + "/" + w + " (" + overs(b) + " ov)";
    }
}
