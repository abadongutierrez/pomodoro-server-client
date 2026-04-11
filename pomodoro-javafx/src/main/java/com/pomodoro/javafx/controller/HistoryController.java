package com.pomodoro.javafx.controller;

import com.pomodoro.javafx.model.HistoryEntry;
import com.pomodoro.javafx.service.TimerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/history")
public class HistoryController {

    private final TimerService timerService;

    public HistoryController(TimerService timerService) {
        this.timerService = timerService;
    }

    @GetMapping
    public List<HistoryEntry> getHistory() {
        return timerService.getHistory();
    }
}
