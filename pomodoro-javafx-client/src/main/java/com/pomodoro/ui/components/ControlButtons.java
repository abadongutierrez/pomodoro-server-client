package com.pomodoro.ui.components;

import com.pomodoro.model.TimerStatus;
import com.pomodoro.ui.Theme;
import com.pomodoro.viewmodel.TimerViewModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class ControlButtons extends HBox {

    public ControlButtons(TimerViewModel vm) {
        setAlignment(Pos.CENTER);
        setSpacing(12);

        Button startPauseBtn = new Button("Start");
        startPauseBtn.setFont(Theme.buttonFont());
        startPauseBtn.setPrefWidth(120);
        startPauseBtn.setPrefHeight(44);

        Button resetBtn = new Button("Reset");
        resetBtn.setFont(Theme.buttonFont());
        resetBtn.setPrefWidth(100);
        resetBtn.setPrefHeight(44);

        // Bind label
        startPauseBtn.textProperty().bind(
            Bindings.when(vm.statusProperty().isEqualTo(TimerStatus.RUNNING))
                .then("Pause")
                .otherwise("Start")
        );

        // Disable Start when time is 00:00 and not running
        startPauseBtn.disableProperty().bind(
            vm.timeDisplayProperty().isEqualTo("00:00")
                .and(vm.statusProperty().isNotEqualTo(TimerStatus.RUNNING))
        );

        // Apply accent-based styling reactively
        vm.modeProperty().addListener((obs, o, mode) -> {
            applyPrimaryStyle(startPauseBtn, Theme.accentHexFor(mode));
        });
        applyPrimaryStyle(startPauseBtn, Theme.accentHexFor(vm.getMode()));
        applySecondaryStyle(resetBtn);

        startPauseBtn.setOnAction(e -> {
            if (vm.getStatus() == TimerStatus.RUNNING) {
                vm.requestPause();
            } else {
                vm.requestStart();
            }
        });

        resetBtn.setOnAction(e -> vm.requestReset());

        getChildren().addAll(startPauseBtn, resetBtn);
    }

    private static void applyPrimaryStyle(Button btn, String accentHex) {
        btn.setStyle(
            "-fx-background-color: " + accentHex + ";" +
            "-fx-text-fill: #ffffff;" +
            "-fx-background-radius: 22;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;"
        );
    }

    private static void applySecondaryStyle(Button btn) {
        btn.setStyle(
            "-fx-background-color: #16213e;" +
            "-fx-text-fill: #eeeeee;" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: #444466;" +
            "-fx-border-radius: 22;" +
            "-fx-cursor: hand;"
        );
    }
}
