package com.pomodoro.javafx.controller;

import com.pomodoro.javafx.model.TimerMode;
import com.pomodoro.javafx.model.TimerState;
import com.pomodoro.javafx.model.TimerStatus;
import com.pomodoro.javafx.service.BlockedSitesService;
import com.pomodoro.javafx.service.ConfigService;
import com.pomodoro.javafx.service.SseEmitterService;
import com.pomodoro.javafx.service.TimerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/timer")
public class TimerController {

    private final TimerService timerService;
    private final BlockedSitesService blockedSitesService;
    private final SseEmitterService sseService;
    private final ConfigService configService;

    public TimerController(TimerService timerService,
                           BlockedSitesService blockedSitesService,
                           SseEmitterService sseService,
                           ConfigService configService) {
        this.timerService = timerService;
        this.blockedSitesService = blockedSitesService;
        this.sseService = sseService;
        this.configService = configService;
    }

    @GetMapping
    public TimerState getState() {
        return timerService.getState();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        SseEmitter emitter = sseService.createEmitter();
        sseService.sendInitial(emitter, timerService.getState(), blockedSitesService.getSites(), configService.getConfig());
        return emitter;
    }

    @PatchMapping
    public ResponseEntity<?> patch(@RequestBody Map<String, String> body) {
        String status = body.get("status");
        String mode   = body.get("mode");

        if (!"running".equals(status) && !"paused".equals(status) && !"idle".equals(status)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "status must be \"running\", \"paused\", or \"idle\""));
        }
        if (mode != null) {
            try {
                TimerMode.fromValue(mode);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "mode must be \"work\", \"short\", or \"long\""));
            }
        }

        TimerState newState;
        if ("idle".equals(status) && mode != null) {
            newState = timerService.setMode(TimerMode.fromValue(mode));
        } else {
            newState = switch (TimerStatus.fromValue(status)) {
                case RUNNING -> timerService.startTimer();
                case PAUSED  -> timerService.pauseTimer();
                case IDLE    -> timerService.resetTimer();
            };
        }
        return ResponseEntity.ok(newState);
    }
}
