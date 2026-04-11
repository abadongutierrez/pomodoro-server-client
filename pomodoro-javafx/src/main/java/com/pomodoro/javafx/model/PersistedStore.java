package com.pomodoro.javafx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PersistedStore {

    @JsonProperty("current")
    private TimerState current;

    @JsonProperty("history")
    private List<HistoryEntry> history = new ArrayList<>();

    public PersistedStore() {}

    public PersistedStore(TimerState current, List<HistoryEntry> history) {
        this.current = current;
        this.history = history;
    }

    public TimerState getCurrent() { return current; }
    public List<HistoryEntry> getHistory() { return history; }

    public void setCurrent(TimerState current) { this.current = current; }
    public void setHistory(List<HistoryEntry> history) { this.history = history; }
}
