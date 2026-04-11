package com.pomodoro.javafx.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TimerMode {
    WORK("work"),
    SHORT("short"),
    LONG("long");

    private final String value;

    TimerMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimerMode fromValue(String value) {
        for (TimerMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) return mode;
        }
        throw new IllegalArgumentException("Unknown TimerMode: " + value);
    }
}
