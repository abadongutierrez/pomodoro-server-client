package com.pomodoro.javafx.viewmodel;

import com.pomodoro.javafx.event.BlockedSitesChangedEvent;
import com.pomodoro.javafx.event.TimerStateChangedEvent;
import com.pomodoro.javafx.model.TimerMode;
import com.pomodoro.javafx.model.TimerState;
import com.pomodoro.javafx.model.TimerStatus;
import com.pomodoro.javafx.service.BlockedSitesService;
import com.pomodoro.javafx.service.TimerService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TimerViewModel {

    private final TimerService timerService;
    private final BlockedSitesService blockedSitesService;

    // Observable properties
    private final SimpleStringProperty timeDisplay = new SimpleStringProperty("25:00");
    private final SimpleDoubleProperty progress    = new SimpleDoubleProperty(0.0);
    private final SimpleStringProperty statusText  = new SimpleStringProperty("Ready");
    private final SimpleObjectProperty<TimerMode>   mode   = new SimpleObjectProperty<>(TimerMode.WORK);
    private final SimpleObjectProperty<TimerStatus> status = new SimpleObjectProperty<>(TimerStatus.IDLE);
    private final ObservableList<String> blockedSites = FXCollections.observableArrayList();

    // Local tick (client-side countdown for smooth display between server syncs)
    private int localRemaining = 1500;
    private int localDuration  = 1500;
    private Timeline localTick; // created in initialize() — requires JavaFX toolkit

    // Guard: events before initialize() are ignored (toolkit not yet started)
    private volatile boolean initialized = false;

    public TimerViewModel(TimerService timerService, BlockedSitesService blockedSitesService) {
        this.timerService = timerService;
        this.blockedSitesService = blockedSitesService;
    }

    // Called by MainStage.show() on the JavaFX Application thread
    public void initialize() {
        localTick = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (localRemaining > 1) {
                localRemaining--;
                timeDisplay.set(formatTime(localRemaining));
                progress.set(computeProgress(localRemaining, localDuration));
            } else {
                localRemaining = 0;
                timeDisplay.set("00:00");
                progress.set(1.0);
                ((Timeline) e.getSource()).stop();
                status.set(TimerStatus.IDLE);
                statusText.set("Done!");
            }
        }));
        localTick.setCycleCount(Timeline.INDEFINITE);

        initialized = true;

        applyState(timerService.getState());
        applyBlockedSites(blockedSitesService.getSites());
    }

    // -------------------------------------------------------------------------
    // Spring event listeners — fire from scheduler/HTTP threads
    // -------------------------------------------------------------------------

    @EventListener
    public void onTimerStateChanged(TimerStateChangedEvent event) {
        if (!initialized) return;
        Platform.runLater(() -> applyState(event.getState()));
    }

    @EventListener
    public void onBlockedSitesChanged(BlockedSitesChangedEvent event) {
        if (!initialized) return;
        Platform.runLater(() -> applyBlockedSites(event.getSites()));
    }

    // -------------------------------------------------------------------------
    // User action methods — called from JavaFX Application thread
    // -------------------------------------------------------------------------

    public void requestStart() {
        timerService.startTimer();
    }

    public void requestPause() {
        timerService.pauseTimer();
    }

    public void requestReset() {
        timerService.resetTimer();
    }

    public void requestModeChange(TimerMode newMode) {
        timerService.setMode(newMode);
    }

    public void saveBlockedSites(String rawText) {
        List<String> sites = Arrays.stream(rawText.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        blockedSitesService.setSites(sites);
    }

    // -------------------------------------------------------------------------
    // Property accessors
    // -------------------------------------------------------------------------

    public StringProperty timeDisplayProperty() { return timeDisplay; }
    public DoubleProperty progressProperty()     { return progress; }
    public StringProperty statusTextProperty()   { return statusText; }
    public ObjectProperty<TimerMode> modeProperty()   { return mode; }
    public ObjectProperty<TimerStatus> statusProperty() { return status; }
    public ObservableList<String> getBlockedSites() { return blockedSites; }

    public TimerMode getMode()     { return mode.get(); }
    public TimerStatus getStatus() { return status.get(); }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void applyState(TimerState s) {
        localRemaining = s.getRemaining();
        localDuration  = s.getDuration();
        timeDisplay.set(formatTime(s.getRemaining()));
        progress.set(computeProgress(s.getRemaining(), s.getDuration()));
        mode.set(s.getMode());
        status.set(s.getStatus());
        statusText.set(deriveStatusText(s));

        if (s.getStatus() == TimerStatus.RUNNING) {
            if (localTick.getStatus() != Animation.Status.RUNNING) localTick.play();
        } else {
            localTick.stop();
        }
    }

    private void applyBlockedSites(List<String> sites) {
        blockedSites.setAll(sites);
    }

    private static String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static double computeProgress(int remaining, int duration) {
        if (duration <= 0) return 0.0;
        return 1.0 - ((double) remaining / duration);
    }

    private static String deriveStatusText(TimerState s) {
        return switch (s.getStatus()) {
            case RUNNING -> switch (s.getMode()) {
                case WORK  -> "Focus";
                case SHORT -> "Short Break";
                case LONG  -> "Long Break";
            };
            case PAUSED -> "Paused";
            case IDLE   -> "Ready";
        };
    }
}
