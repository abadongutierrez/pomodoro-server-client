package com.pomodoro.javafx.ui.components;

import com.pomodoro.javafx.ui.Theme;
import com.pomodoro.javafx.viewmodel.TimerViewModel;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;

public class ProgressRing extends StackPane {

    private static final double RADIUS = 90;
    private static final double STROKE = 10;
    private static final double SIZE   = (RADIUS + STROKE) * 2;

    public ProgressRing(TimerViewModel vm) {
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);

        Arc trackArc = new Arc(SIZE / 2, SIZE / 2, RADIUS, RADIUS, 0, 360);
        trackArc.setType(ArcType.OPEN);
        trackArc.setFill(null);
        trackArc.setStroke(Theme.SURFACE);
        trackArc.setStrokeWidth(STROKE);

        Arc progressArc = new Arc(SIZE / 2, SIZE / 2, RADIUS, RADIUS, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(null);
        progressArc.setStroke(Theme.accentFor(vm.getMode()));
        progressArc.setStrokeWidth(STROKE);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);
        progressArc.lengthProperty().bind(vm.progressProperty().multiply(-360.0));

        Text timeText = new Text();
        timeText.setFont(Theme.timerFont());
        timeText.setFill(Theme.TEXT_PRIMARY);
        timeText.textProperty().bind(vm.timeDisplayProperty());

        vm.modeProperty().addListener((obs, oldMode, newMode) ->
            progressArc.setStroke(Theme.accentFor(newMode))
        );

        getChildren().addAll(trackArc, progressArc, timeText);
    }
}
