package com.pomodoro.ui.components;

import com.pomodoro.model.TimerMode;
import com.pomodoro.ui.Theme;
import com.pomodoro.viewmodel.TimerViewModel;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public class ModeTabs extends HBox {

    public ModeTabs(TimerViewModel vm) {
        setAlignment(Pos.CENTER);
        setSpacing(4);

        ToggleGroup group = new ToggleGroup();

        ToggleButton workBtn  = makeTab("Focus",       TimerMode.WORK,  group, vm);
        ToggleButton shortBtn = makeTab("Short Break",  TimerMode.SHORT, group, vm);
        ToggleButton longBtn  = makeTab("Long Break",   TimerMode.LONG,  group, vm);

        // Keep tab selection in sync with server state
        vm.modeProperty().addListener((obs, oldMode, newMode) -> {
            switch (newMode) {
                case WORK  -> workBtn.setSelected(true);
                case SHORT -> shortBtn.setSelected(true);
                case LONG  -> longBtn.setSelected(true);
            }
            updateStyles(group, vm.getMode());
        });

        // Set initial selection
        workBtn.setSelected(true);
        updateStyles(group, vm.getMode());

        getChildren().addAll(workBtn, shortBtn, longBtn);
    }

    private ToggleButton makeTab(String label, TimerMode mode, ToggleGroup group, TimerViewModel vm) {
        ToggleButton btn = new ToggleButton(label);
        btn.setToggleGroup(group);
        btn.setFont(Theme.labelFont());
        btn.setStyle(inactiveStyle());

        btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                btn.setStyle(activeStyle(mode));
                vm.requestModeChange(mode);
            } else {
                btn.setStyle(inactiveStyle());
            }
        });

        return btn;
    }

    private void updateStyles(ToggleGroup group, TimerMode currentMode) {
        group.getToggles().forEach(toggle -> {
            ToggleButton btn = (ToggleButton) toggle;
            if (toggle.isSelected()) {
                btn.setStyle(activeStyle(currentMode));
            } else {
                btn.setStyle(inactiveStyle());
            }
        });
    }

    private static String inactiveStyle() {
        return "-fx-background-color: transparent;" +
               "-fx-text-fill: #888888;" +
               "-fx-background-radius: 20;" +
               "-fx-border-color: transparent;" +
               "-fx-padding: 6 14 6 14;" +
               "-fx-cursor: hand;";
    }

    private static String activeStyle(TimerMode mode) {
        String accent = Theme.accentHexFor(mode);
        return "-fx-background-color: " + accent + "33;" +
               "-fx-text-fill: " + accent + ";" +
               "-fx-background-radius: 20;" +
               "-fx-border-color: " + accent + ";" +
               "-fx-border-radius: 20;" +
               "-fx-padding: 6 14 6 14;" +
               "-fx-cursor: hand;";
    }
}
