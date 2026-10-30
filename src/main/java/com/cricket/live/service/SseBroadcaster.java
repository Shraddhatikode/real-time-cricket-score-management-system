package com.cricket.live.service;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Fan-out of score events to all connected dashboards (Server-Sent Events). */
@Component
public class SseBroadcaster {
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public SseEmitter register() {
        SseEmitter e = new SseEmitter(0L);
        emitters.add(e);
        e.onCompletion(() -> emitters.remove(e));
        e.onTimeout(() -> emitters.remove(e));
        e.onError(t -> emitters.remove(e));
        return e;
    }

    public void broadcast(String event, Object data) {
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                emitters.remove(e);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        for (SseEmitter e : emitters) {
            try { e.send(SseEmitter.event().comment("hb")); } catch (Exception ex) { emitters.remove(e); }
        }
    }
}
