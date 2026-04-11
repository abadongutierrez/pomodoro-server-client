package com.pomodoro.javafx.service;

import com.pomodoro.javafx.event.BlockedSitesChangedEvent;
import com.pomodoro.javafx.event.TimerStateChangedEvent;
import com.pomodoro.javafx.model.AppConfig;
import com.pomodoro.javafx.model.TimerState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEmitterService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        return emitter;
    }

    public void sendInitial(SseEmitter emitter, TimerState state, List<String> sites, AppConfig config) {
        try {
            emitter.send(SseEmitter.event().name("timerUpdate").data(state));
            emitter.send(SseEmitter.event().name("blockedSitesUpdate").data(sites));
            emitter.send(SseEmitter.event().name("configUpdate").data(config));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    @EventListener
    public void onTimerStateChanged(TimerStateChangedEvent event) {
        broadcast("timerUpdate", event.getState());
    }

    @EventListener
    public void onBlockedSitesChanged(BlockedSitesChangedEvent event) {
        broadcast("blockedSitesUpdate", event.getSites());
    }

    private void broadcast(String eventName, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
