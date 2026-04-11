package com.pomodoro.javafx.ui;

import com.pomodoro.javafx.viewmodel.TimerViewModel;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class MainStage {

    private final TimerViewModel vm;

    public MainStage(TimerViewModel vm) {
        this.vm = vm;
    }

    public void show(Stage primaryStage) {
        vm.initialize();
        new MainWindow(vm, primaryStage).show();
    }
}
