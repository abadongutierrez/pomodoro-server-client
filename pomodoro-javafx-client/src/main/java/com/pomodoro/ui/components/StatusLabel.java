package com.pomodoro.ui.components;

import com.pomodoro.ui.Theme;
import com.pomodoro.viewmodel.TimerViewModel;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class StatusLabel extends HBox {

    public StatusLabel(TimerViewModel vm) {
        setAlignment(Pos.CENTER);

        Text label = new Text("Ready");
        label.setFont(Theme.labelFont());
        label.setFill(Theme.TEXT_MUTED);
        label.textProperty().bind(vm.statusTextProperty());

        getChildren().add(label);
    }
}
