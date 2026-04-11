package com.pomodoro.javafx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.javafx.event.TimerStateChangedEvent;
import com.pomodoro.javafx.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TimerService {

    private static final Map<TimerMode, Integer> DURATIONS = new EnumMap<>(TimerMode.class);
    static {
        DURATIONS.put(TimerMode.WORK,  25 * 60);
        DURATIONS.put(TimerMode.SHORT,  5 * 60);
        DURATIONS.put(TimerMode.LONG,  15 * 60);
    }

    private static final int HISTORY_LIMIT = 100;
    private static final Path STATE_DIR  = Path.of(System.getProperty("user.home"), ".pomodoro");
    private static final Path STATE_FILE = STATE_DIR.resolve("pomodoro-state.json");

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final boolean ticksEnabled;

    // Internal mutable state — all mutations must be synchronized
    private TimerState state;
    private final List<HistoryEntry> history = new ArrayList<>();

    public TimerService(ApplicationEventPublisher eventPublisher,
                        ObjectMapper objectMapper,
                        @Value("${pomodoro.sse.ticks-enabled:true}") boolean ticksEnabled) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.ticksEnabled = ticksEnabled;
        loadStore();
    }

    // -------------------------------------------------------------------------
    // Scheduled expiry watcher (every 1 second)
    // -------------------------------------------------------------------------

    @Scheduled(fixedDelay = 1000)
    public synchronized void watchExpiry() {
        if (state.getStatus() != TimerStatus.RUNNING) return;

        if (computeRemaining(state) <= 0) {
            advanceAfterExpiry();
            eventPublisher.publishEvent(new TimerStateChangedEvent(this, buildResponse()));
        } else if (ticksEnabled) {
            eventPublisher.publishEvent(new TimerStateChangedEvent(this, buildResponse()));
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public synchronized TimerState getState() {
        // Lazy expiry — handles the case where the watcher hasn't fired yet
        if (state.getStatus() == TimerStatus.RUNNING && computeRemaining(state) <= 0) {
            advanceAfterExpiry();
        }
        return buildResponse();
    }

    public synchronized List<HistoryEntry> getHistory() {
        List<HistoryEntry> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        return reversed;
    }

    public synchronized TimerState startTimer() {
        if (state.getStatus() == TimerStatus.RUNNING) return buildResponse();
        state.setStatus(TimerStatus.RUNNING);
        state.setStartedAt(System.currentTimeMillis());
        persistStore();
        TimerState response = buildResponse();
        eventPublisher.publishEvent(new TimerStateChangedEvent(this, response));
        return response;
    }

    public synchronized TimerState pauseTimer() {
        if (state.getStatus() != TimerStatus.RUNNING) return buildResponse();
        long sinceStart = (System.currentTimeMillis() - state.getStartedAt()) / 1000;
        state.setElapsed(state.getElapsed() + (int) sinceStart);
        state.setStatus(TimerStatus.PAUSED);
        state.setStartedAt(null);
        persistStore();
        TimerState response = buildResponse();
        eventPublisher.publishEvent(new TimerStateChangedEvent(this, response));
        return response;
    }

    public synchronized TimerState resetTimer() {
        state.setStatus(TimerStatus.IDLE);
        state.setStartedAt(null);
        state.setElapsed(0);
        state.setSessionId(UUID.randomUUID().toString());
        persistStore();
        TimerState response = buildResponse();
        eventPublisher.publishEvent(new TimerStateChangedEvent(this, response));
        return response;
    }

    public synchronized TimerState setMode(TimerMode mode) {
        state.setMode(mode);
        state.setDuration(DURATIONS.get(mode));
        state.setStatus(TimerStatus.IDLE);
        state.setStartedAt(null);
        state.setElapsed(0);
        state.setSessionId(UUID.randomUUID().toString());
        persistStore();
        TimerState response = buildResponse();
        eventPublisher.publishEvent(new TimerStateChangedEvent(this, response));
        return response;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void advanceAfterExpiry() {
        recordHistory(state);
        TimerMode nextMode = computeNextMode(state.getMode());
        state.setMode(nextMode);
        state.setDuration(DURATIONS.get(nextMode));
        state.setStatus(TimerStatus.IDLE);
        state.setStartedAt(null);
        state.setElapsed(0);
        state.setSessionId(UUID.randomUUID().toString());
        persistStore();
    }

    private void recordHistory(TimerState completed) {
        HistoryEntry entry = new HistoryEntry();
        entry.setSessionId(completed.getSessionId());
        entry.setMode(completed.getMode());
        entry.setDuration(completed.getDuration());
        entry.setCompletedAt(System.currentTimeMillis());
        history.add(entry);
        if (history.size() > HISTORY_LIMIT) history.remove(0);
    }

    // Pomodoro sequence: long→work, work→short, short→work (or long after 3 shorts)
    private TimerMode computeNextMode(TimerMode completedMode) {
        if (completedMode == TimerMode.LONG)  return TimerMode.WORK;
        if (completedMode == TimerMode.WORK)  return TimerMode.SHORT;
        // Count short breaks since last long break
        int shortBreakCount = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).getMode() == TimerMode.LONG)  break;
            if (history.get(i).getMode() == TimerMode.SHORT) shortBreakCount++;
        }
        return shortBreakCount >= 3 ? TimerMode.LONG : TimerMode.WORK;
    }

    private TimerState buildResponse() {
        TimerState r = copy(state);
        r.setRemaining(computeRemaining(state));
        return r;
    }

    private static int computeRemaining(TimerState s) {
        if (s.getStatus() == TimerStatus.RUNNING && s.getStartedAt() != null) {
            long sinceStart = (System.currentTimeMillis() - s.getStartedAt()) / 1000;
            return (int) Math.max(0, s.getDuration() - s.getElapsed() - sinceStart);
        }
        return Math.max(0, s.getDuration() - s.getElapsed());
    }

    private static TimerState copy(TimerState s) {
        TimerState c = new TimerState();
        c.setStatus(s.getStatus());
        c.setMode(s.getMode());
        c.setDuration(s.getDuration());
        c.setElapsed(s.getElapsed());
        c.setStartedAt(s.getStartedAt());
        c.setSessionId(s.getSessionId());
        c.setRemaining(s.getRemaining());
        return c;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private void loadStore() {
        try {
            if (Files.exists(STATE_FILE)) {
                String json = Files.readString(STATE_FILE);
                // Detect new vs legacy format
                if (json.contains("\"current\"")) {
                    PersistedStore store = objectMapper.readValue(json, PersistedStore.class);
                    state = recoverCurrent(store.getCurrent());
                    if (store.getHistory() != null) {
                        history.addAll(store.getHistory());
                    }
                    return;
                } else {
                    // Legacy: bare TimerState
                    state = recoverCurrent(objectMapper.readValue(json, TimerState.class));
                    return;
                }
            }
        } catch (IOException e) {
            // Corrupted file — fall through to defaults
        }
        state = defaultState();
    }

    private void persistStore() {
        try {
            Files.createDirectories(STATE_DIR);
            PersistedStore store = new PersistedStore(state, history);
            Files.writeString(STATE_FILE, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(store));
        } catch (IOException e) {
            // Non-fatal — continue without persistence
        }
    }

    private static TimerState recoverCurrent(TimerState saved) {
        // If server was killed while running, transition to paused to preserve elapsed
        if (saved.getStatus() == TimerStatus.RUNNING) {
            long sinceStart = saved.getStartedAt() != null
                ? (System.currentTimeMillis() - saved.getStartedAt()) / 1000
                : 0;
            saved.setStatus(TimerStatus.PAUSED);
            saved.setStartedAt(null);
            saved.setElapsed(Math.min(saved.getElapsed() + (int) sinceStart, saved.getDuration()));
        }
        return saved;
    }

    private static TimerState defaultState() {
        TimerState s = new TimerState();
        s.setStatus(TimerStatus.IDLE);
        s.setMode(TimerMode.WORK);
        s.setDuration(DURATIONS.get(TimerMode.WORK));
        s.setStartedAt(null);
        s.setElapsed(0);
        s.setSessionId(UUID.randomUUID().toString());
        return s;
    }
}
