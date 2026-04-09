package com.pomodoro.ui.components;

import com.pomodoro.ui.Theme;
import com.pomodoro.viewmodel.TimerViewModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public class ConnectionBadge extends HBox {

    public ConnectionBadge(TimerViewModel vm) {
        setAlignment(Pos.CENTER_RIGHT);
        setSpacing(6);

        Circle dot = new Circle(5);
        dot.fillProperty().bind(
            Bindings.when(vm.serverOnlineProperty())
                .then(Theme.SHORT_ACCENT)
                .otherwise((Color) Theme.TEXT_MUTED)
        );

        Text label = new Text("offline");
        label.setFont(Theme.labelFont());
        label.fillProperty().bind(
            Bindings.when(vm.serverOnlineProperty())
                .then(Theme.SHORT_ACCENT)
                .otherwise((Color) Theme.TEXT_MUTED)
        );
        label.textProperty().bind(
            Bindings.when(vm.serverOnlineProperty())
                .then("online")
                .otherwise("offline")
        );

        getChildren().addAll(dot, label);
    }
}
