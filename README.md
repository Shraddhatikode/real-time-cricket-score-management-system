# Cricket Live – real-time score management (Spring Boot)

Java 17 · Spring Boot 3.3 · Spring Data JPA (H2) · Server-Sent Events · vanilla-JS dashboard

## Run
```bash
mvn spring-boot:run        # then open http://localhost:8080
mvn test                   # plays a full match through the service layer
```
Three matches are seeded (two LIVE, one UPCOMING). `LiveSimulator` bowls one ball per live
match every `simulation.tick-ms` (default 2000). Set `simulation.enabled=false` to drive scores
only through the API.

## REST API
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/matches?status=LIVE` | list matches with scoreboard view |
| GET | `/api/matches/{id}` | one match (score, overs, CRR/RRR, batters, bowler) |
| POST | `/api/matches` | `{teamA, teamB, venue, overs}` create |
| POST | `/api/matches/{id}/start` | start innings 1 |
| POST | `/api/matches/{id}/ball` | `{runs, extra: WIDE\|NO_BALL\|null, wicket}` manual score update |
| GET | `/api/matches/{id}/commentary?limit=30` | latest balls |
| GET | `/api/matches/{id}/summary` | innings scores, 4s/6s, extras, top scorer, fall of wickets |
| GET | `/api/players?team=` · `/api/players/leaders` · `/api/teams` | player stats, leaderboards |
| GET | `/api/stream` | SSE; event `score` = `{match, ball}` after every delivery |

## Design notes
- **Ball log as source of truth** (`BallEvent`): commentary, batter/bowler figures and summaries derive from it.
- **Per-match locking** keeps one match's deliveries ordered while different matches score in parallel.
- **SSE push** instead of polling: one `/stream` connection per dashboard, heartbeat every 15 s.
- **Scaling out**: swap H2 for PostgreSQL, replace `SseBroadcaster` with Redis pub/sub (or Kafka) so every
  instance fans out to its own clients, replace the in-JVM lock with `SELECT … FOR UPDATE` / `@Version`,
  and put the live-state view behind a cache. Replace `LiveSimulator` with a real feed client.
