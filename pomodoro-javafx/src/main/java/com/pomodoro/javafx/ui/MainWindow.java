package com.pomodoro.javafx.ui;

import com.pomodoro.javafx.ui.components.*;
import com.pomodoro.javafx.viewmodel.TimerViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class MainWindow {

    private final TimerViewModel vm;
    private final Stage stage;

    public MainWindow(TimerViewModel vm, Stage stage) {
        this.vm = vm;
        this.stage = stage;
    }

    public void show() {
        Text titleText = new Text("Pomodoro");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleText.setFill(Theme.TEXT_PRIMARY);

        VBox center = new VBox(20,
            titleText,
            new ModeTabs(vm),
            new ProgressRing(vm),
            new ControlButtons(vm),
            new StatusLabel(vm),
            new BlockedSitesPanel(vm)
        );
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(24, 20, 24, 20));

        BorderPane root = new BorderPane(center);
        root.setStyle("-fx-background-color: #1a1a2e;");

        stage.setTitle("Pomodoro");
        stage.setScene(new Scene(root, 360, 600));
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
    }
}
