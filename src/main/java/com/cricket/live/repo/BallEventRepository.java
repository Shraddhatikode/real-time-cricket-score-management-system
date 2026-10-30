package com.cricket.live.repo;

import com.cricket.live.model.BallEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BallEventRepository extends JpaRepository<BallEvent, Long> {
    List<BallEvent> findByMatchIdAndInnings(Long matchId, int innings);
    List<BallEvent> findByMatchIdOrderByIdAsc(Long matchId);
    List<BallEvent> findByMatchIdOrderByIdDesc(Long matchId, Pageable page);
}
