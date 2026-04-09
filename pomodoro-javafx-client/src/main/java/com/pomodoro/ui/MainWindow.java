package com.pomodoro.ui;

import com.pomodoro.ui.components.*;
import com.pomodoro.viewmodel.TimerViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
        // Title row
        Text titleText = new Text("Pomodoro");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleText.setFill(Theme.TEXT_PRIMARY);

        ConnectionBadge badge = new ConnectionBadge(vm);

        HBox titleRow = new HBox(titleText, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setSpacing(0);
        HBox.setHgrow(badge, javafx.scene.layout.Priority.ALWAYS);
        badge.setMaxWidth(Double.MAX_VALUE);

        // Core UI stack
        ModeTabs modeTabs = new ModeTabs(vm);
        ProgressRing ring = new ProgressRing(vm);
        ControlButtons controls = new ControlButtons(vm);
        StatusLabel statusLabel = new StatusLabel(vm);
        BlockedSitesPanel blockedSites = new BlockedSitesPanel(vm);

        VBox center = new VBox(20,
            titleRow,
            modeTabs,
            ring,
            controls,
            statusLabel,
            blockedSites
        );
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(24, 20, 24, 20));

        BorderPane root = new BorderPane(center);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 360, 600);

        stage.setTitle("Pomodoro");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
    }
}
