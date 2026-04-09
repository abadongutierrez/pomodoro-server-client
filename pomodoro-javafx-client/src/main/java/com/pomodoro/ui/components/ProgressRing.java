package com.pomodoro.ui.components;

import com.pomodoro.ui.Theme;
import com.pomodoro.viewmodel.TimerViewModel;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;

public class ProgressRing extends StackPane {

    private static final double RADIUS = 90;
    private static final double STROKE = 10;
    private static final double SIZE   = (RADIUS + STROKE) * 2;

    private final Arc progressArc;

    public ProgressRing(TimerViewModel vm) {
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);

        // Background track ring (full 360°)
        Arc trackArc = new Arc(SIZE / 2, SIZE / 2, RADIUS, RADIUS, 0, 360);
        trackArc.setType(ArcType.OPEN);
        trackArc.setFill(null);
        trackArc.setStroke(Theme.SURFACE);
        trackArc.setStrokeWidth(STROKE);

        // Progress arc: starts at 90° (top), sweeps counter-clockwise
        progressArc = new Arc(SIZE / 2, SIZE / 2, RADIUS, RADIUS, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(null);
        progressArc.setStroke(Theme.accentFor(vm.getMode()));
        progressArc.setStrokeWidth(STROKE);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);

        // Bind arc sweep to progress: 0.0 → 0°, 1.0 → -360°
        progressArc.lengthProperty().bind(vm.progressProperty().multiply(-360.0));

        // Time text centered on ring
        Text timeText = new Text("25:00");
        timeText.setFont(Theme.timerFont());
        timeText.setFill(Theme.TEXT_PRIMARY);
        timeText.textProperty().bind(vm.timeDisplayProperty());

        // Update accent color when mode changes
        vm.modeProperty().addListener((obs, oldMode, newMode) ->
            progressArc.setStroke(Theme.accentFor(newMode))
        );

        getChildren().addAll(trackArc, progressArc, timeText);
    }
}
