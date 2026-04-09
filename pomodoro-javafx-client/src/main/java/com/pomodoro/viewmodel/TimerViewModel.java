package com.pomodoro.viewmodel;

import com.pomodoro.model.TimerMode;
import com.pomodoro.model.TimerState;
import com.pomodoro.model.TimerStatus;
import com.pomodoro.service.PomodoroApiService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TimerViewModel {

    private final PomodoroApiService apiService;

    // Display properties
    private final SimpleStringProperty timeDisplay = new SimpleStringProperty("25:00");
    private final SimpleDoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final SimpleStringProperty statusText = new SimpleStringProperty("Ready");
    private final SimpleBooleanProperty serverOnline = new SimpleBooleanProperty(false);

    // Domain state
    private final SimpleObjectProperty<TimerMode> mode = new SimpleObjectProperty<>(TimerMode.WORK);
    private final SimpleObjectProperty<TimerStatus> status = new SimpleObjectProperty<>(TimerStatus.IDLE);

    // Blocked sites
    private final ObservableList<String> blockedSites = FXCollections.observableArrayList();

    // Local tick state
    private int localRemaining = 1500;
    private int localDuration = 1500;
    private final Timeline localTick;

    public TimerViewModel(PomodoroApiService apiService) {
        this.apiService = apiService;

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
    }

    // --- Called by SseService (from background thread) ---

    public void applyServerState(TimerState s) {
        Platform.runLater(() -> {
            localRemaining = s.getRemaining();
            localDuration = s.getDuration();
            timeDisplay.set(formatTime(s.getRemaining()));
            progress.set(computeProgress(s.getRemaining(), s.getDuration()));
            mode.set(s.getMode());
            status.set(s.getStatus());
            statusText.set(deriveStatusText(s));
            serverOnline.set(true);

            if (s.getStatus() == TimerStatus.RUNNING) {
                if (localTick.getStatus() != Animation.Status.RUNNING) {
                    localTick.play();
                }
            } else {
                localTick.stop();
            }
        });
    }

    public void applyBlockedSites(List<String> sites) {
        Platform.runLater(() -> blockedSites.setAll(sites));
    }

    public void setServerOnline(boolean online) {
        Platform.runLater(() -> {
            serverOnline.set(online);
            if (!online) {
                localTick.stop();
            }
        });
    }

    // --- User action methods ---

    public void requestStart() {
        apiService.patchTimer("running", null)
            .thenAccept(this::applyServerState)
            .exceptionally(ex -> null);
    }

    public void requestPause() {
        apiService.patchTimer("paused", null)
            .thenAccept(this::applyServerState)
            .exceptionally(ex -> null);
    }

    public void requestReset() {
        apiService.patchTimer("idle", null)
            .thenAccept(this::applyServerState)
            .exceptionally(ex -> null);
    }

    public void requestModeChange(TimerMode newMode) {
        apiService.patchTimer("idle", newMode.getValue())
            .thenAccept(this::applyServerState)
            .exceptionally(ex -> null);
    }

    public void saveBlockedSites(String rawText) {
        List<String> sites = Arrays.stream(rawText.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        apiService.putBlockedSites(sites)
            .thenAccept(this::applyBlockedSites)
            .exceptionally(ex -> null);
    }

    // --- Property accessors ---

    public StringProperty timeDisplayProperty() { return timeDisplay; }
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty statusTextProperty() { return statusText; }
    public BooleanProperty serverOnlineProperty() { return serverOnline; }
    public ObjectProperty<TimerMode> modeProperty() { return mode; }
    public ObjectProperty<TimerStatus> statusProperty() { return status; }
    public ObservableList<String> getBlockedSites() { return blockedSites; }

    public TimerMode getMode() { return mode.get(); }
    public TimerStatus getStatus() { return status.get(); }
    public boolean isServerOnline() { return serverOnline.get(); }

    // --- Helpers ---

    private static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private static double computeProgress(int remaining, int duration) {
        if (duration <= 0) return 0.0;
        return 1.0 - ((double) remaining / duration);
    }

    private static String deriveStatusText(TimerState s) {
        return switch (s.getStatus()) {
            case RUNNING -> switch (s.getMode()) {
                case WORK -> "Focus";
                case SHORT -> "Short Break";
                case LONG -> "Long Break";
            };
            case PAUSED -> "Paused";
            case IDLE -> "Ready";
        };
    }
}
