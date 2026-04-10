package com.pomodoro.javafx.ui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class MainStage {

    public void show(Stage primaryStage) {
        Label label = new Label("Pomodoro JavaFX + Spring Boot");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("Pomodoro JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
