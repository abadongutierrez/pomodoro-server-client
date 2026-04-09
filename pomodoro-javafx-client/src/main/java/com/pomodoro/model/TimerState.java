package com.pomodoro.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimerState {

    @JsonProperty("status")
    private TimerStatus status;

    @JsonProperty("mode")
    private TimerMode mode;

    @JsonProperty("duration")
    private int duration;

    @JsonProperty("remaining")
    private int remaining;

    @JsonProperty("elapsed")
    private int elapsed;

    @JsonProperty("startedAt")
    private Long startedAt;

    @JsonProperty("sessionId")
    private String sessionId;

    public TimerState() {}

    public TimerStatus getStatus() { return status; }
    public TimerMode getMode() { return mode; }
    public int getDuration() { return duration; }
    public int getRemaining() { return remaining; }
    public int getElapsed() { return elapsed; }
    public Long getStartedAt() { return startedAt; }
    public String getSessionId() { return sessionId; }

    public void setStatus(TimerStatus status) { this.status = status; }
    public void setMode(TimerMode mode) { this.mode = mode; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setRemaining(int remaining) { this.remaining = remaining; }
    public void setElapsed(int elapsed) { this.elapsed = elapsed; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
