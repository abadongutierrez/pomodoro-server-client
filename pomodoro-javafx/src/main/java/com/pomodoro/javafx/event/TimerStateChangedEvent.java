package com.pomodoro.javafx.event;

import com.pomodoro.javafx.model.TimerState;
import org.springframework.context.ApplicationEvent;

public class TimerStateChangedEvent extends ApplicationEvent {

    private final TimerState state;

    public TimerStateChangedEvent(Object source, TimerState state) {
        super(source);
        this.state = state;
    }

    public TimerState getState() {
        return state;
    }
}
