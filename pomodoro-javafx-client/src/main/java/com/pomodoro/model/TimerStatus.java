package com.pomodoro.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TimerStatus {
    IDLE("idle"),
    RUNNING("running"),
    PAUSED("paused");

    private final String value;

    TimerStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimerStatus fromValue(String value) {
        for (TimerStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown TimerStatus: " + value);
    }
}
