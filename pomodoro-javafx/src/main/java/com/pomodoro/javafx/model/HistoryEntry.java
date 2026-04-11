package com.pomodoro.javafx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryEntry {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("mode")
    private TimerMode mode;

    @JsonProperty("duration")
    private int duration;

    @JsonProperty("completedAt")
    private long completedAt;

    public HistoryEntry() {}

    public String getSessionId() { return sessionId; }
    public TimerMode getMode() { return mode; }
    public int getDuration() { return duration; }
    public long getCompletedAt() { return completedAt; }

    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setMode(TimerMode mode) { this.mode = mode; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
}
