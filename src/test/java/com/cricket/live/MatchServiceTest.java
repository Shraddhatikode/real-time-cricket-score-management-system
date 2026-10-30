package com.cricket.live;

import com.cricket.live.dto.Dto;
import com.cricket.live.service.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "simulation.enabled=false")
class MatchServiceTest {
    @Autowired MatchService service;

    @Test
    void playsAFullMatchToCompletion() {
        var v = service.create(new Dto.CreateMatch("Delhi Dynamos", "Chennai Chargers", "Test Ground", 2));
        service.start(v.id());
        for (int i = 0; i < 200 && !"COMPLETED".equals(service.get(v.id()).status()); i++)
            service.recordBall(v.id(), new Dto.BallInput(1, null, false));
        var done = service.get(v.id());
        assertEquals("COMPLETED", done.status());
        assertNotNull(done.result());
        assertEquals(12, done.target() - 1);   // first innings: 12 balls x 1 run
    }
}
